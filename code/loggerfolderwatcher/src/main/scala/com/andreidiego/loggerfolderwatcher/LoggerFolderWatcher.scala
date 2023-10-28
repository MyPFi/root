package com.andreidiego.loggerfolderwatcher

import java.nio.file.{Files, Path}
import org.slf4j.LoggerFactory
import scala.sys.addShutdownHook
import scala.util.{Failure, Success}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import akka.{Done, NotUsed}
import akka.util.Timeout
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{KillSwitches, UniqueKillSwitch}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.{Directory, DirectoryChangesSource}

private var brokerageNotesFolderRegistry: Map[Path, UniqueKillSwitch] = Map.empty
private val rootFolder = Path.of("G:/Workspace/bntest")

private val log = LoggerFactory.getLogger("Main")

@main def runStream(args: String*): Unit =
  log.debug("Main -> Initializing the ActorSystem...")
  given system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "User-Guardian")

  given Timeout = Timeout(3.seconds)
  given ExecutionContext  = system.executionContext
  log.debug("Main -> ActorSystem initialized.")

  val existingFolderSource: Source[Path, NotUsed] =
    Directory.walk(rootFolder, Some(1))
  log.debug("Main -> Directory Source created for {}...", rootFolder)

  val preExistingFolderWalker: Future[Done] =
    existingFolderSource.runForeach { resourcePath =>
      log.debug("Main -> New resource found {}.", resourcePath)

      if Files.isDirectory(resourcePath) then
        log.debug("Main -> {} is a folder. It needs to be watched.", resourcePath)

        watch(resourcePath)
      else
        log.debug("Main -> {} is a file.", resourcePath)
    }

  preExistingFolderWalker.onComplete { _ =>
    log.debug("Main -> Done walking {}.", rootFolder)
  }

  system.whenTerminated.onComplete {
    case Failure(exception) ⇒
      log.debug("Main -> Failure detected when shutting down the actor system: {}", exception.getMessage)
      sys.exit(1)
    case Success(_) ⇒
      log.debug("Main -> Actor system successfully shut down.")
      sys.exit()
  }

  addShutdownHook {
    log.debug("Main -> Shutting down application...")
    log.debug("Main -> Shutting down the actor system...")
    system.terminate()
    // Perform any cleanup operations here
  }

def watch(folder: Path)(using system: ActorSystem[Nothing], ec: ExecutionContext): Unit =
  val changeSource: Source[(Path, DirectoryChange), NotUsed] =
    DirectoryChangesSource(
      folder, pollInterval = 1.second, maxBufferSize = 1000
    )
  val changeSink: Sink[(Path, DirectoryChange), Future[Done]] = Sink
    .foreach[(Path, DirectoryChange)] { case (changedPreExistingFolderPath, change) =>
      change match
        case DirectoryChange.Modification ⇒
          log.debug("Main -> Something changed under {}: {}", folder, changedPreExistingFolderPath)
        case DirectoryChange.Creation ⇒
          log.debug("Main -> New resource {} found under {}.", changedPreExistingFolderPath, folder)

          if Files.isDirectory(changedPreExistingFolderPath) then
            log.debug("Main -> {} is a folder. It needs to be watched.", changedPreExistingFolderPath)

            watch(changedPreExistingFolderPath)
          else
            log.debug("Main -> {} is a file.", changedPreExistingFolderPath)

        case DirectoryChange.Deletion ⇒
          log.debug("Main -> A resource {} has been removed from {}", changedPreExistingFolderPath, folder)

          val possibleKillSwitch: Option[UniqueKillSwitch] =
            brokerageNotesFolderRegistry.get(changedPreExistingFolderPath)

          possibleKillSwitch match
            case Some(_) ⇒
              log.debug("Main -> {} was a folder.", changedPreExistingFolderPath)

              val subFolders: Map[Path, UniqueKillSwitch] = brokerageNotesFolderRegistry
                .filter((folder, _) ⇒ folder.startsWith(changedPreExistingFolderPath))

              log.debug("Main -> Removing folder {} and its subfolders {}", changedPreExistingFolderPath, subFolders.keys.mkString(", "))
              subFolders.foreach { (_, killSwitch) ⇒ killSwitch.shutdown() }

              brokerageNotesFolderRegistry = brokerageNotesFolderRegistry.removedAll(subFolders.keys)
              log.debug("Main -> Registry after deletion: {}", brokerageNotesFolderRegistry)

            case None ⇒
              log.debug("Main -> {} was a file.", changedPreExistingFolderPath)
    }

  val (
    killSwitch: UniqueKillSwitch,
    changesDone: Future[Done]
  ) = changeSource
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(changeSink)(Keep.both)
    .run()

  changesDone.onComplete { _ =>
    if folder == rootFolder then
      log.debug("Main -> {}, which is the root folder, has been removed so, I'll need to terminate the system.", rootFolder)
      system.terminate()

    log.debug("Main -> changesDone completed for {}", folder)
  }

  brokerageNotesFolderRegistry = brokerageNotesFolderRegistry + (folder → killSwitch)
  log.debug("Main -> Registry after creation: {}", brokerageNotesFolderRegistry)