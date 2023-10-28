package com.andreidiego.filewatcher

import java.nio.file.{Files, Path}
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import akka.{Done, NotUsed}
import akka.util.Timeout
import akka.actor.ActorInitializationException
import akka.actor.typed.{ActorRef, Behavior, ChildFailed, MessageAdaptionFailure}
import akka.actor.typed.{PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.persistence.typed.{DeleteEventsCompleted, DeleteEventsFailed, DeleteSnapshotsCompleted}
import akka.persistence.typed.{DeleteSnapshotsFailed, EventSourcedSignal, RecoveryCompleted}
import akka.persistence.typed.{RecoveryFailed, SnapshotCompleted, SnapshotFailed}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.{Directory, DirectoryChangesSource}
import util.{CborSerializable, RequestId}

object FolderWatcher:
  private val log = LoggerFactory.getLogger(getClass)

  private enum AckType(
    val successMessage: (ActorRef[_], ActorRef[_], Path, Any) ⇒ String,
    val errorMessage: (ActorRef[_], ActorRef[_], Path, Throwable) ⇒ String
  ):
    case NowWatching extends AckType(
      (party1, party2, what, ack) ⇒
        s"${AckType.confirmationMessage(party1, party2)} am now watching $what. Ack: $ack",
      (party1, party2, what, failure) ⇒
        s"${AckType.pendingMessage(party1, party2)} am now watching $what. Failure: $failure"
    )
    case NewFile extends AckType(
      (party1, party2, what, ack) ⇒
        s"${AckType.confirmationMessage(party1, party2)} have found a new file $what. Ack: $ack",
      (party1, party2, what, failure) ⇒
        s"${AckType.pendingMessage(party1, party2)} have found a new file $what. Ack: $failure"
    )
    case FileRemoved extends AckType(
      (party1, party2, what, ack) ⇒
        s"${AckType.confirmationMessage(party1, party2)} have reported $what is now gone. Ack: $ack",
      (party1, party2, what, failure) ⇒
        s"${AckType.pendingMessage(party1, party2)} have reported $what is now gone. Ack: $failure"
    )
  private object AckType:
    private val confirmationMessage: (ActorRef[_], ActorRef[_]) => String =
      (party1, party2) ⇒ s"$party1 confirms it is aware that I ($party2)"
    private val pendingMessage: (ActorRef[_], ActorRef[_]) => String =
      (party1, party2) ⇒ s"$party1 still hasn't acknowledge that I ($party2)"

  sealed trait Protocol extends CborSerializable

  sealed trait Command extends Protocol
  private final case class HandleFileSystemWatcherAck(
    ackType : AckType,
    result  : Try[_],
    resource: Path,
    command : AckCommand
  )                                                                         extends Command
  private sealed trait AckCommand                                           extends Command
  private final case class AckWatching(folder: Path, requestId : RequestId) extends AckCommand
  private final case class AckNewFile(file: Path)                           extends AckCommand
  private final case class AckFileRemoved(file: Path)                       extends AckCommand

  def apply(
    folder            : Path,
    requestId         : RequestId,
    fileSystemWatcher : ActorRef[FileSystemWatcher.Command]
  )(using Timeout): Behavior[Command] =
    Behaviors.supervise {
//      Behaviors.supervise {
      Behaviors.setup[Command] { context =>
        Behaviors.withTimers[Command] { timerScheduler ⇒
          context.log.debug("FolderWatcher -> Initializing behavior...")
          context.log.debug("FolderWatcher -> Instantiating a Stream Materializer...")

          given Materializer = Materializer(context)
          given ExecutionContext = context.executionContext

          context.log.debug("FolderWatcher -> Scheduling notification to let {} know that I'm on it...", fileSystemWatcher)

          timerScheduler.startSingleTimer(
            "ack-now-watching",
            AckWatching(folder, requestId),
            10.millis
          )

          context.log.debug("FolderWatcher -> Instantiating a Directory Source for {}...", folder)

          val existingFolderSource: Source[Path, NotUsed] =
            Directory.walk(folder, Some(1))
          context.log.debug("FolderWatcher -> Directory Source created for {}...", folder)

          val preExistingFolderWalker: Future[Done] =
            existingFolderSource.runForeach { resourcePath =>
              context.log.debug("FolderWatcher -> New folder found under {}: {}.", folder, resourcePath)

              if Files.isDirectory(resourcePath) then
                context.log.debug("FolderWatcher -> {} is a Folder.", resourcePath)
                context.log.debug("FolderWatcher -> Instantiating a child FolderWatcher actor for folder {}...", resourcePath)

                val childFolderWatcher = FolderWatcher(
                  resourcePath, requestId, fileSystemWatcher
                )
                context.log.debug("FolderWatcher -> Spawning a child FolderWatcher actor for folder {}...", resourcePath)

                context.spawn(
                  childFolderWatcher,
                  s"ChildFolderWatcher-$resourcePath"
                )
              // TODO Maybe I'll have to watch all my children and only
              //  emit AckNowWatching to FileSystemWatcher after I collect
              //  the AckNowWatching from all of them. Right now, if one
              //  of them fail to initialize (because, for example, the
              //  folder it was supposed to watch was removed in between)
              //  nobody knows because nobody is watching. In case of a
              //  failure like that, I should probably retry a couple times
              //  and, if no luck, let it crash al the way up so that the
              //  whole structure can be rebuilt with a fresh look of the filesystem.
              else
                context.log.debug("FolderWatcher -> Scheduling notification to let {} know that I have found a new file {}...", fileSystemWatcher, resourcePath)

                timerScheduler.startSingleTimer(
                  s"ack-new-file-$resourcePath",
                  AckNewFile(resourcePath),
                  10.millis
                )
            }

          preExistingFolderWalker.onComplete { _ =>
            log.debug("FolderWatcher -> Done walking {}.", folder)
            //        system.terminate()
          }

          val changeSource: Source[(Path, DirectoryChange), NotUsed] =
            DirectoryChangesSource(
              folder, pollInterval = 1.second, maxBufferSize = 1000
            )

          val changeSink: Sink[(Path, DirectoryChange), Future[Done]] =
            Sink.foreach[(Path, DirectoryChange)] { case (changedPreExistingFolderPath, change) =>
              change match
                case DirectoryChange.Modification ⇒
                  context.log.debug("FolderWatcher -> DirectoryChange.Modification -> Path: $changedPreExistingFolderPath")

                case DirectoryChange.Creation ⇒
                  context.log.debug("FolderWatcher -> DirectoryChange.Creation -> Path: {}", changedPreExistingFolderPath)
                  context.log.debug("FolderWatcher -> Files.isDirectory(changedPreExistingFolderPath): {}", Files.isDirectory(changedPreExistingFolderPath))

                  if Files.isDirectory(changedPreExistingFolderPath) then
                    context.log.debug("FolderWatcher -> {} is a Folder.", changedPreExistingFolderPath)
                    context.log.debug("FolderWatcher -> Instantiating a child FolderWatcher actor for folder {}...", changedPreExistingFolderPath)

                    val childFolderWatcher = FolderWatcher(
                      changedPreExistingFolderPath, requestId, fileSystemWatcher
                    )
                    context.log.debug("FolderWatcher -> Spawning a child FolderWatcher actor for folder {}...", changedPreExistingFolderPath)

                    context.spawn(
                      childFolderWatcher,
                      s"ChildFolderWatcher-$changedPreExistingFolderPath"
                    )
                  // TODO Same as above
                  else
                    context.log.debug("FolderWatcher -> Scheduling notification to let {} know that I have found a new file {}...", fileSystemWatcher, changedPreExistingFolderPath)

                    timerScheduler.startSingleTimer(
                      s"ack-new-file-$changedPreExistingFolderPath",
                      AckNewFile(changedPreExistingFolderPath),
                      10.millis
                    )

                case DirectoryChange.Deletion ⇒
                  context.log.debug("FolderWatcher -> DirectoryChange.Deletion -> Path: {}", changedPreExistingFolderPath)

                  if Files.isDirectory(changedPreExistingFolderPath) then
                  // TODO What happens if the user removes a folder that is
                  //  being watched? Does the stream get terminated with a
                  //  specific failure? Does the actor that contains the
                  //  stream crash. Will it keep trying to restart? Is
                  //  stopping self a possibility?
                    context.stop(context.self)
                  else
                    context.log.debug("FolderWatcher -> Scheduling notification to let {} know that the file {} has been deleted...", fileSystemWatcher, changedPreExistingFolderPath)

                    timerScheduler.startSingleTimer(
                      s"ack-file-removed-$changedPreExistingFolderPath",
                      AckFileRemoved(changedPreExistingFolderPath),
                      10.millis
                    )
            }

          val eventualChange: Future[Done] = changeSource.runWith(changeSink)
          eventualChange.onComplete { _ =>
            log.debug("FolderWatcher -> changesDone completed for: {}", folder)
            //        system.terminate()
          }

          Behaviors.receiveMessage[Command] {

            case ackWatching @ AckWatching(folder, requestId) ⇒
              context.log.debug("FolderWatcher -> Notifying {} that I'm now watching {}...", fileSystemWatcher, folder)

              context.askWithStatus(
                fileSystemWatcher,
                FileSystemWatcher.AckNowWatching(
                  folder, context.self, requestId, _
                )
              ) {
                HandleFileSystemWatcherAck(
                  AckType.NowWatching, _, folder, ackWatching
                )
              }

              Behaviors.same

            case ackNewFile @ AckNewFile(file) ⇒
              context.log.debug("FolderWatcher -> Notifying {} that I've found a new file {}...", fileSystemWatcher, file)

              context.askWithStatus(
                fileSystemWatcher,
                FileSystemWatcher.AckNewFile(file, _)
              ) {
                HandleFileSystemWatcherAck(
                  AckType.NewFile, _, file, ackNewFile
                )
              }

              Behaviors.same

            case ackFileRemoved @ AckFileRemoved(file) ⇒
              context.log.debug("FolderWatcher -> Notifying {} that the file {} has been deleted...", fileSystemWatcher, file)

              context.askWithStatus(
                fileSystemWatcher,
                FileSystemWatcher.AckFileRemoved(file, _)
              ) {
                HandleFileSystemWatcherAck(
                  AckType.FileRemoved, _, file, ackFileRemoved
                )
              }

              Behaviors.same

            case HandleFileSystemWatcherAck(
            ackType, result, resource, command
            ) ⇒ result match
              case Success(ack) ⇒
                context.log.debug("FolderWatcher -> {}", ackType.successMessage(fileSystemWatcher, context.self, resource, ack))

              case Failure(exception) ⇒
                context.log.debug("FolderWatcher -> {}", ackType.errorMessage(fileSystemWatcher, context.self, resource, exception))

                timerScheduler.startSingleTimer(
                  "retry-command",
                  command,
                  10.millis
                )

              Behaviors.same

          }.receiveSignal {
            case (_, signal: PreRestart) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: PostStop) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: RecoveryFailed) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: RecoveryCompleted) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: ChildFailed) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: Terminated) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: MessageAdaptionFailure) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: SnapshotFailed) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: SnapshotCompleted) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: DeleteSnapshotsFailed) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: DeleteSnapshotsCompleted) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: DeleteEventsFailed) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: DeleteEventsCompleted) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: EventSourcedSignal) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
            case (_, signal: Signal) =>
              context.log.debug("FolderWatcher -> Signal received {}.", signal)
              Behaviors.same
          }
        }
      }
      //      }.onFailure[ActorInitializationException] {
      //        SupervisorStrategy.stop
      //      }
    }.onFailure {
      SupervisorStrategy
        .restart
        .withStopChildren(false)
    }