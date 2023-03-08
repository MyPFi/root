package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.filewatcher

import java.nio.file.Path
import org.slf4j.{Logger, LoggerFactory}
import scala.io.StdIn
import scala.sys.addShutdownHook
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import akka.Done
import akka.pattern.StatusReply
import akka.util.Timeout
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.RequestId
import FileSystemWatcher.{FileLifecycleEvent, Request, RequestStatus, getClass}

private val log: Logger = LoggerFactory.getLogger(getClass)
private var acceptedRequests: Seq[RequestId] = Seq.empty

@main def run(args: String*): Unit =
  log.debug("FileSystemWatcher-Run -> Initializing the ActorSystem...")

  System.setProperty(
    "config.resource",
    "/com/andreidiego/mpfi/stocks/adapter/files/watchers/actors/poc/application.conf"
  )
  given system: ActorSystem[UserGuardian.Command] = ActorSystem(
    UserGuardian(),
    "User-Guardian"
  )
  given Timeout           = Timeout(3.seconds)
  given ExecutionContext  = system.executionContext
  log.debug("FileSystemWatcher-Run -> ActorSystem initialized.")

  log.debug("FileSystemWatcher-Run -> Spawning FileSystemWatcher...")
  val eventualFileSystemWatcher: Future[ActorRef[FileSystemWatcher.Command]] =
    system.ask {
      UserGuardian.Spawn(FileSystemWatcher(), "FileSystemWatcher", Props.empty, _)
    }

  log.debug("FileSystemWatcher-Run -> Spawning FilePathPrinter...")
  val eventualFilePathPrinter: Future[ActorRef[FileLifecycleEvent]] =
    system.ask {
      UserGuardian.Spawn(FilePathPrinter(), "FilePathPrinter", Props.empty, _)
    }

  log.debug("FileSystemWatcher-Run -> Building userCommandProcessor...")
  val process = userCommandProcessor(
    eventualFileSystemWatcher,
    eventualFilePathPrinter
  )
  log.debug("FileSystemWatcher-Run -> CommandProcessor ready.")

  log.debug("FileSystemWatcher-Run -> Instantiating NamedPipeServer...")
  val server = new NamedPipeServer
  log.debug("FileSystemWatcher-Run -> Preparing to launch NamedPipeServer...")
  //  server.start(process)
  log.debug("FileSystemWatcher-Run -> NamedPipeServer is up. Ready to receive commands.")

  while (true) {
    val command = StdIn.readLine("Type in the next command: ")
    println()
    process(command)
  }

  system.whenTerminated.onComplete {
    case Failure(exception) ⇒
      log.debug("FileSystemWatcher-Run -> Failure detected when shutting down the actor system: {}", exception.getMessage)
      sys.exit(1)
    case Success(_) ⇒
      log.debug("FileSystemWatcher-Run -> Actor system successfully shut down.")
      sys.exit()
  }

  addShutdownHook {
    log.debug("FileSystemWatcher-Run -> Shutting down application...")
    log.debug("FileSystemWatcher-Run -> Shutting down the actor system...")
    system.terminate()
    // Perform any cleanup operations here
  }

def userCommandProcessor(
  eventualFileSystemWatcher : Future[ActorRef[FileSystemWatcher.Command]],
  eventualFilePathPrinter   : Future[ActorRef[FileLifecycleEvent]]
)(using
  system  : ActorSystem[UserGuardian.Command],
  ec      : ExecutionContext,
  timeout : Timeout
): String ⇒ Unit = message ⇒
  log.debug("FileSystemWatcher-Run -> Received message: {}", message)

  val (command, parameters) = message.span(_ != ' ')

  command match
    case "subscribe" =>
      val folder = Path.of(parameters.replaceAll("\"", "").strip())

      log.debug("FileSystemWatcher-Run -> Subscribing for changes on {}.", folder)

      val result: Future[RequestId] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        filePathPrinter   <- eventualFilePathPrinter
        answer            <- fileSystemWatcher.askWithStatus[RequestId] {
          FileSystemWatcher.SubscribeForChangesOn(folder, filePathPrinter, None, _)
        }
      yield answer

      result.onComplete {
        case Success(requestId) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher accepted the request and assigned it ID {}", requestId)
          acceptedRequests =  requestId +: acceptedRequests

        case Failure(ex)        =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "unsubscribe" =>
      val folder = Path.of(parameters.replaceAll("\"", "").strip())

      log.debug("FileSystemWatcher-Run -> Unsubscribing from changes on {}.", folder)

      val result: Future[RequestId] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        filePathPrinter   <- eventualFilePathPrinter
        answer            <- fileSystemWatcher.askWithStatus[RequestId] {
          FileSystemWatcher.UnsubscribeFromChangesOn(folder, filePathPrinter, None, _)
        }
      yield answer

      result.onComplete {
        case Success(requestId) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher accepted the request and assigned it ID {}", requestId)
          acceptedRequests =  requestId +: acceptedRequests

        case Failure(ex)        =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "requests" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher for an update on previously sent requests.")

      val result = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.ask[Option[RequestStatus] | Map[Request, RequestStatus]] {
          FileSystemWatcher.GetStatus(None, _)
        }
      yield answer

      result.onComplete {
        case Success(requestStatuses) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher has reported the following requests:")

          requestStatuses.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)              =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "subscriptions" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher for the subscription map containing folders subscribed to and their subscribers.")

      val result: Future[Map[Path, Seq[ActorRef[FileLifecycleEvent]]]] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.ask {
          FileSystemWatcher.GetSubscriptions
        }
      yield answer

      result.onComplete {
        case Success(subscriptions) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher has reported the following subscriptions:")

          for (folder, subscribers) <- subscriptions
          do
            log.debug("- {}", folder)
            for subscriber <- subscribers
            do  log.debug("-- {}", subscriber)

        case Failure(ex)            =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "folders" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher for the map of watched folders and their watchers.")

      val result: Future[Map[Path, ActorRef[FolderWatcher.Command]]] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.ask {
          FileSystemWatcher.GetWatchedFolders
        }
      yield answer

      result.onComplete {
        case Success(watchers)  =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher has reported the following folders together with their watchers:")

          watchers.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)        =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "files" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher for the files that have been reported by all watchers.")

      val result: Future[Seq[Path]] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.ask {
          FileSystemWatcher.GetFiles
        }
      yield answer

      result.onComplete {
        case Success(files) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher has reported the following files:")

          files.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)    =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "pendingdeliveries" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher for the deliveries that are still pending.")

      val result: Future[Iterable[(
        Path,
        ActorRef[FileLifecycleEvent],
        ActorRef[StatusReply[Done]] => FileLifecycleEvent
      )]] =
        for
          fileSystemWatcher <- eventualFileSystemWatcher
          answer            <- fileSystemWatcher.ask {
            FileSystemWatcher.GetPendingDeliveries
          }
        yield answer

      result.onComplete {
        case Success(deliveries)  =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher has reported the following pending deliveries:")

          for (folder, subscriber, _) <- deliveries
          do  log.debug("- {} -> {}", folder, subscriber)

        case Failure(ex)          =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }
      
    case "restart" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher to restart.")

      val result: Future[Done] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.askWithStatus {
          FileSystemWatcher.Restart
        }
      yield answer

      result.onComplete {
        case Success(ack) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher got the message and should be restarting soon.")

        case Failure(ex)  =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "stop" =>
      log.debug("FileSystemWatcher-Run -> Asking FileSystemWatcher to stop.")

      val result: Future[Done] = for
        fileSystemWatcher <- eventualFileSystemWatcher
        answer            <- fileSystemWatcher.askWithStatus {
          FileSystemWatcher.Stop
        }
      yield answer

      result.onComplete {
        case Success(ack) =>
          log.debug("FileSystemWatcher-Run -> FileSystemWatcher got the message and should be stopping soon.")

        case Failure(ex)  =>
          log.debug("FileSystemWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case c => log.debug("FileSystemWatcher-Run -> Invalid command: {}", c)

object UserGuardian:
  sealed trait Command
  final case class Spawn[T](
    behavior: Behavior[T],
    name    : String,
    props   : Props,
    replyTo : ActorRef[ActorRef[T]]
  )                                                                 extends Command
  final case class ActOnTerminationOf[T](watchedActor: ActorRef[T]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors.receiveMessage[Command] {

        case Spawn(behavior: Behavior[t], name, props, replyTo) =>
          log.debug("Spawning a new behavior {}...", behavior)

          val ref = context.spawn(behavior, name, props)
          context.watchWith(ref, ActOnTerminationOf(ref))
          replyTo ! ref
          Behaviors.same

        // FIXME I have to figure out why the actor system is
        //  not being stopped when its user guardian is stopped
        case ActOnTerminationOf(ref) =>
          log.debug("{} has terminated and I cannot do anything without it so, I'm stopping too...", ref)
          Behaviors.stopped
      }
    }

object FilePathPrinter:
  def apply(): Behavior[FileLifecycleEvent] =
    Behaviors.receive { (context, message) ⇒ message match

      case FileSystemWatcher.FileCreated(file, ackTo) ⇒
        context.log.debug("FilePathPrinter -> Just heard about the creation of {}.", file)

        ackTo ! StatusReply.ack()
        Behaviors.same
      case FileSystemWatcher.FileRemoved(file, ackTo) ⇒
        context.log.debug("FilePathPrinter -> Just heard about the removal of {}.", file)

        ackTo ! StatusReply.ack()
        Behaviors.same
    }