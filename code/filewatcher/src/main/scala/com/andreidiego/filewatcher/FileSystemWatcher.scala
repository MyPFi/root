package com.andreidiego.filewatcher

import java.util.UUID
import java.nio.file.Path
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt
import akka.Done
import akka.pattern.StatusReply
import akka.util.Timeout
import akka.actor.ActorInitializationException
import akka.actor.typed.{ActorRef, Behavior, ChildFailed, MessageAdaptionFailure}
import akka.actor.typed.{PostStop, PreRestart, Signal, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.persistence.typed.{DeleteEventsCompleted, DeleteEventsFailed, DeleteSnapshotsCompleted}
import akka.persistence.typed.{DeleteSnapshotsFailed, EventSourcedSignal, PersistenceId}
import akka.persistence.typed.{RecoveryCompleted, RecoveryFailed, SnapshotCompleted, SnapshotFailed}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect, RetentionCriteria}
import util.{CborSerializable, RequestId}

object FileSystemWatcher:

  sealed trait Protocol extends CborSerializable

  // There is a module (jackson-module-scala3-enum) that claims
  // to be able to deserialize scala 3 enums but I can't use it
  // because both akka-persistence-jdbc and akka-stream-alpakka-file
  // are stuck in scala 2.13. So, until something happens, every new
  // enum that needs to be persisted will require its own
  // serializer/deserializer pair.
  // TODO I'll try to solve this with reflection later.
  @JsonSerialize(`using`    = classOf[RequestTypeSerializer])
  @JsonDeserialize(`using`  = classOf[RequestTypeDeserializer])
  enum RequestType extends Protocol:
    case SubscribeForChangesOn, UnsubscribeFromChangesOn

  @JsonSerialize(`using`    = classOf[RequestStatusSerializer])
  @JsonDeserialize(`using`  = classOf[RequestStatusDeserializer])
  enum RequestStatus extends Protocol:
    case Accepted
    case Fulfilled
    case Failed(ex: Throwable)
  object RequestStatus:
    def fromString(s: String): Option[RequestStatus] = s match
      case "Accepted"   ⇒ Some(Accepted)
      case "Fulfilled"  ⇒ Some(Fulfilled)
      case _            ⇒ None
  final case class Request(
    requestId   : RequestId,
    requestType : RequestType,
    folder      : Path,
    sender      : ActorRef[StatusReply[RequestId]],
    onBehalfOf  : ActorRef[FileLifecycleEvent]
  )

  sealed trait Command extends Protocol
  final case class SubscribeForChangesOn(
    folder            : Path,
    subscriber        : ActorRef[FileLifecycleEvent],
    optionalRequestId : Option[RequestId],
    ackTo             : ActorRef[StatusReply[RequestId]]
  )                                                                           extends Command
  private final case class IfSubfolderWatchersTerminatedThenWatch(
    path      : Path,
    requestId : RequestId
  )                                                                           extends Command
  final case class AckNowWatching(
    path      : Path,
    watcher   : ActorRef[FolderWatcher.Command],
    requestId : RequestId,
    ackTo     : ActorRef[StatusReply[Done]]
  )                                                                           extends Command
  final case class UnsubscribeFromChangesOn(
    folder            : Path,
    subscriber        : ActorRef[FileLifecycleEvent],
    optionalRequestId : Option[RequestId],
    ackTo             : ActorRef[StatusReply[RequestId]]
  )                                                                           extends Command
  private final case class RemoveWatcherOf(path: Path, requestId: RequestId)  extends Command
  final case class AckNewFile(
    path  : Path,
    ackTo : ActorRef[StatusReply[Done]]
  )                                                                           extends Command
  final case class AckFileRemoved(
    path  : Path,
    ackTo : ActorRef[StatusReply[Done]]
  )                                                                           extends Command
  private final case class DeliverInstallment(
    subscriber    : ActorRef[FileLifecycleEvent],
    lifecycleEvent: ActorRef[StatusReply[Done]] ⇒ FileLifecycleEvent,
    file          : Path
  )                                                                           extends Command
  private case class HandleInstallmentDeliverySignature(
    signature     : Try[Done],
    subscriber    : ActorRef[FileLifecycleEvent],
    lifecycleEvent: ActorRef[StatusReply[Done]] ⇒ FileLifecycleEvent,
    file          : Path
  )                                                                           extends Command
  final case class GetStatus(
    requestId : Option[RequestId] = None,
    replyTo   : ActorRef[Option[RequestStatus] | Map[Request, RequestStatus]]
  )                                                                           extends Command
  final case class GetSubscriptions(
    replyTo: ActorRef[Map[Path, Seq[ActorRef[FileLifecycleEvent]]]]
  )                                                                           extends Command
  final case class GetWatchedFolders(
    replyTo: ActorRef[Map[Path, ActorRef[FolderWatcher.Command]]]
  )                                                                           extends Command
  final case class GetFiles(
    replyTo: ActorRef[Seq[Path]]
  )                                                                           extends Command
  final case class GetPendingDeliveries(
    replyTo: ActorRef[Iterable[(
      Path,
        ActorRef[FileLifecycleEvent],
        ActorRef[StatusReply[Done]] => FileLifecycleEvent
      )]]
  )                                                                           extends Command
  case class Restart(ackTo: ActorRef[StatusReply[Done]])                      extends Command
  case class Stop(ackTo: ActorRef[StatusReply[Done]])                         extends Command
  private case object ResendPendingRequestsAndDeliveries                      extends Command

  private sealed trait Event extends Protocol
  private final case class RequestAccepted(
    requestId   : RequestId,
    requestType : RequestType,
    folder      : Path,
    sender      : ActorRef[StatusReply[RequestId]],
    onBehalfOf  : ActorRef[FileLifecycleEvent]
  )                                                                                     extends Event
  private final case class Subscribed(
    subscriber: ActorRef[FileLifecycleEvent],
    path      : Path
  )                                                                                     extends Event
  private final case class FolderWatcherConfirmedWatching(
    path    : Path,
    watcher : ActorRef[FolderWatcher.Command]
  )                                                                                     extends Event
  private final case class Unsubscribed(
    subscriber: ActorRef[FileLifecycleEvent],
    path      : Path
  )                                                                                     extends Event
  private final case class FolderWatcherTerminated(path: Path)                          extends Event
  private final case class RequestCompleted(
    requestId: RequestId,
    requestStatus: RequestStatus
  )                                                                                     extends Event
  private final case class FolderWatcherFoundNewFile(path: Path)                        extends Event
  private final case class FolderWatcherSaysFileRemoved(path: Path)                     extends Event
  private final case class InstallmentDeliverySigned(
    file      : Path,
    subscriber: ActorRef[FileLifecycleEvent]
  )                                                                                     extends Event

  sealed trait Response extends Protocol
  sealed trait FileLifecycleEvent extends Response
  final case class FileCreated(file: Path, ackTo: ActorRef[StatusReply[Done]]) extends FileLifecycleEvent
  final case class FileRemoved(file: Path, ackTo: ActorRef[StatusReply[Done]]) extends FileLifecycleEvent

  final case class State(
    requestStatuses   : Map[Request, RequestStatus],
    subscriptions     : Map[Path, Seq[ActorRef[FileLifecycleEvent]]],
    watchedFolders    : Map[Path, ActorRef[FolderWatcher.Command]],
    files             : Seq[Path],
    pendingDeliveries : Iterable[(
      Path,
      ActorRef[FileLifecycleEvent],
      ActorRef[StatusReply[Done]] => FileLifecycleEvent
    )]
  ) extends Protocol:
    def withRequest(request: Request): State = State(
      requestStatuses + (request → RequestStatus.Accepted),
      subscriptions,
      watchedFolders,
      files,
      pendingDeliveries
    )
    def updateStatus(requestId: RequestId, status: RequestStatus): State =
      val existingRequest: Request = requestStatuses
        .find((request: Request, _: RequestStatus) ⇒ request.requestId == requestId)
        .map(_._1)
        .getOrElse(throw RuntimeException(
          s"FileSystemWatcher's state does not seem to have the request (Id $requestId) whose status needs to be updated to $status. This should never happen."
        ))
      State(
        requestStatuses + (existingRequest → status),
        subscriptions,
        watchedFolders,
        files,
        pendingDeliveries
      )
    def statusOf(requestId: RequestId): Option[RequestStatus] = requestStatuses
      .find { (request: Request, _: RequestStatus) =>
        request.requestId == requestId
      }.map(_._2)
    def forAllPendingRequests(execute: Request ⇒ Unit): Unit = requestStatuses
      .filter(_._2 == RequestStatus.Accepted)
      .foreach((request, _) ⇒ execute(request))

    def withSubscription(
      subscriber: ActorRef[FileLifecycleEvent],
      path      : Path
    ): State = State(
      requestStatuses,
      subscriptions + (path → (subscriber +: subscriptions.getOrElse(path, Seq.empty))),
      watchedFolders,
      files,
      pendingDeliveries
    )
    def withoutSubscription(
      subscriber: ActorRef[FileLifecycleEvent],
      path      : Path
    ): State = State(
      requestStatuses,
      subscriptions.updatedWith(path) {
        _.map {
          _.filterNot(existingSubscriber ⇒ existingSubscriber == subscriber)
        }
      },
      watchedFolders,
      files,
      pendingDeliveries
    )
    def noSubscriberLeft(to: Path)(
      otherThan: ActorRef[FileLifecycleEvent]
    ): Boolean = subscriptions
      .get(to)
      .map(_.filterNot(_ == otherThan).isEmpty)
      .getOrElse(true)
    def highestSubfoldersStillSubscribedToBelow(
      folder: Path
    ): Iterable[Path] = subscriptions
      .filter { (watchedFolder, _) =>
        watchedFolder.startsWith(folder) && watchedFolder != folder
      }
      .groupBy { (watchedFolder, _) =>
        watchedFolder.getNameCount
      }
      .toSeq
      .minBy(_._1)
      ._2
      .keys

    def watch(
      path    : Path,
      watcher : ActorRef[FolderWatcher.Command]
    ): State = State(
      requestStatuses,
      subscriptions,
      watchedFolders + (path → watcher),
      files,
      pendingDeliveries
    )
    def unwatch(path: Path): State = State(
      requestStatuses,
      subscriptions,
      watchedFolders - path,
      files,
      pendingDeliveries
    )
    def isWatching(path: Path): Boolean = watchedFolders
      .exists((watchedPath, _) ⇒ path.startsWith(watchedPath))
    def isNotWatching(path: Path): Boolean = !isWatching(path)
    def alreadyWatchingSubfoldersOf(path: Path): Boolean = watchedFolders
      .exists { (watchedPath, _) =>
        watchedPath.startsWith(path) && !path.startsWith(watchedPath)
      }
    def notWatchingSubfoldersOf(path: Path): Boolean =
      !alreadyWatchingSubfoldersOf(path)
    def forEachWatchedSubfolderOf(path: Path)(
      action: ((Path, ActorRef[FolderWatcher.Command])) ⇒ Unit
    ): Unit =
      watchedFolders
        .filter { (watchedPath, _) =>
          watchedPath.startsWith(path) && watchedPath != path
        }
        .foreach(action)
    def watcherOf(
      path: Path
    ): Option[ActorRef[FolderWatcher.Command]] = watchedFolders
      .find { (watchedPath, _) =>
        path.startsWith(watchedPath)
      }.map(_._2)

    def withFile(file: Path): State = State(
      requestStatuses,
      subscriptions,
      watchedFolders,
      file +: files,
      pendingDeliveries
    )
    def withoutFile(file: Path): State = State(
      requestStatuses,
      subscriptions,
      watchedFolders,
      files.filterNot(_ != file),
      pendingDeliveries
    )
    def has(file: Path): Boolean = files.contains(file)
    def doesNotHave(file: Path): Boolean = !has(file)

    def withPendingDeliveries(
      file          : Path,
      lifecycleEvent: ActorRef[StatusReply[Done]] => FileLifecycleEvent
    ): State =
      val subscribers = subscriptions
        .filter((subscribedFolder, _) ⇒ file.startsWith(subscribedFolder))
        .values
        .flatten

      State(
        requestStatuses,
        subscriptions,
        watchedFolders,
        files,
        if subscribers.isEmpty then
          pendingDeliveries
        else
          subscribers.map {
            (file, _, lifecycleEvent)
          } ++ pendingDeliveries
      )
    def withDeliverySigned(
      file      : Path,
      subscriber: ActorRef[FileLifecycleEvent]
    ): State = State(
      requestStatuses,
      subscriptions,
      watchedFolders,
      files,
      pendingDeliveries.filterNot { (pendingFile, sendTo, _) =>
        pendingFile == file && sendTo == subscriber
      }
    )
    def forAllPendingDeliveriesTo(file: Path)(execute: (
      ActorRef[FileLifecycleEvent],
      ActorRef[StatusReply[Done]] => FileLifecycleEvent
    ) => Unit): Unit = pendingDeliveries
      .filter { (pendingFile, _, _) =>
        pendingFile == file
      }
      .foreach { (_, sendTo, lifecycleEvent) =>
        execute(sendTo, lifecycleEvent)
      }
    def forEachPendingDelivery(
      execute: (
        Path,
        ActorRef[FileLifecycleEvent],
        ActorRef[StatusReply[Done]] => FileLifecycleEvent
      ) => Unit
    ): Unit = pendingDeliveries
      .foreach { (file, subscriber, lifecycleEvent) =>
        execute(file, subscriber, lifecycleEvent)
      }

  object State:
    val empty: State = State(
      Map.empty, Map.empty, Map.empty, Seq.empty, Iterable.empty
    )

  private def handleCommand(
    state         : State,
    command       : Command,
    context       : ActorContext[Command],
    timerScheduler: TimerScheduler[Command]
  )(using Timeout): ReplyEffect[Event, State] =
    context.log.debug("FileSystemWatcher -> Received command {}.", command)

    command match
      case SubscribeForChangesOn(
        folder, subscriber, optionalRequestId, ackTo
      ) ⇒
        context.log.debug("FileSystemWatcher -> {} wants to subscribe {} for changes to {}", ackTo, subscriber, folder)

        val requestId = optionalRequestId
          .getOrElse(RequestId(UUID.randomUUID()))
        context.log.debug("FileSystemWatcher -> {}.", requestId)

        Effect
          .persist[Event, State] {
            val requestAccepted = RequestAccepted(
              requestId,
              RequestType.SubscribeForChangesOn,
              folder,
              ackTo,
              subscriber
            )
            context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", requestAccepted)

            val subscribed = Subscribed(subscriber, folder)
            context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", subscribed)

            Seq(requestAccepted, subscribed)
          }
          .thenRun { state ⇒
            context.log.debug("FileSystemWatcher -> Making sure I'm already watching {}. Spawning FolderWatchers otherwise...", folder)

            if state.isNotWatching(folder) then
              context.log.debug("FileSystemWatcher -> Preparing to watch folder: {}.", folder)
              context.log.debug("FileSystemWatcher -> Checking if subfolders of {} are already being watched...", folder)

              if state.notWatchingSubfoldersOf(folder) then
                context.log.debug("FileSystemWatcher -> Confirmed that no subfolders of {} are being watched.", folder)

                // TODO Evaluate the need to handle timeouts when spawning
                //  actors in the same way that we handle timeouts when
                //  communicating with FileSystemWatcher from BrokerageNoteWatcher
                watch(folder)(requestId, context)
              else
                context.log.debug("FileSystemWatcher -> Detected subfolders of {} already being watched.", folder)

                state.forEachWatchedSubfolderOf(folder) { (subfolder, watcher) ⇒
                  context.log.debug("FileSystemWatcher -> Stopping FolderWatcher actor {} that is currently watching {}...", watcher, subfolder)

                  context.stop(watcher)
                }
                context.log.debug("FileSystemWatcher -> Waiting for FolderWatcher actors that are currently watching {} to stop...", folder)

                timerScheduler.startSingleTimer(
                  "wait-for-child-termination-from-subscribe-for-changes-on",
                  IfSubfolderWatchersTerminatedThenWatch(folder, requestId),
                  500.millis
                )
            else
              context.log.debug("FileSystemWatcher -> It looks like I'm already watching {} so, this is it for {}.", folder, requestId)

              Effect
                .persist[Event, State] {
                  val requestCompleted = RequestCompleted(
                    requestId,
                    RequestStatus.Fulfilled
                  )
                  context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", requestCompleted)

                  requestCompleted
                }
          }
          .thenReply(ackTo) { _ ⇒
            context.log.debug("FileSystemWatcher -> Replying back to the SubscribeForChangesOn request from {} with the requestId {}", ackTo, requestId)
            StatusReply.success(requestId)
          }

      case IfSubfolderWatchersTerminatedThenWatch(path, requestId) =>
        context.log.debug("FileSystemWatcher -> Checking if any subfolder of {} is still being watched...", path)

        if state.notWatchingSubfoldersOf(path) then
          context.log.debug("FileSystemWatcher -> Confirmed that no subfolders of {} are being watched anymore.", path)

          watch(path)(requestId, context)
        else
          context.log.debug("FileSystemWatcher -> Waiting for FolderWatcher actors that are currently watching {} subfolders to stop...", path)

          timerScheduler.startSingleTimer(
            "wait-for-child-termination",
            IfSubfolderWatchersTerminatedThenWatch(path, requestId),
            500.millis
          )

        Effect.noReply

      case AckNowWatching(path, watcher, requestId, ackTo) =>
        context.log.debug("FileSystemWatcher -> Heard back from {} about requestId {}. Status: {}.", watcher, requestId, RequestStatus.Fulfilled)
        context.log.debug("FileSystemWatcher -> FolderWatcher {} is now watching {}.", watcher, path)

        Effect
          .persist(
            RequestCompleted(requestId, RequestStatus.Fulfilled),
            FolderWatcherConfirmedWatching(path, watcher)
          )
          .thenReply(ackTo) { _ ⇒
            StatusReply.ack()
          }

      case UnsubscribeFromChangesOn(
        folder, subscriber, optionalRequestId, ackTo
      ) ⇒
        context.log.debug("FileSystemWatcher -> {} wants to unsubscribe {} from changes on {}", ackTo, subscriber, folder)

        val requestId = optionalRequestId
          .getOrElse(RequestId(UUID.randomUUID()))
        context.log.debug("FileSystemWatcher -> {}.", requestId)

        Effect
          .persist[Event, State] {
            val requestAccepted = RequestAccepted(
              requestId,
              RequestType.UnsubscribeFromChangesOn,
              folder,
              ackTo,
              subscriber
            )
            context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", requestAccepted)

            val unsubscribed = Unsubscribed(subscriber, folder)
            context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", unsubscribed)

            Seq(requestAccepted, unsubscribed)
          }
          .thenRun { state ⇒
            context.log.debug("FileSystemWatcher -> Checking if there's still somebody interested in changes from {}...", folder)

            if state.noSubscriberLeft(to = folder)(otherThan = subscriber) then
              val watcher = state.watcherOf(folder)
              context.log.debug("FileSystemWatcher -> It looks like nobody else other than {} is subscribed to {} so, I think it's time to say goodbye to {}.", subscriber, folder, watcher)

              // TODO Do we need to wait here???
              // FIXME I need to handle request completion in this case
              watcher foreach {
                context.stop
              }
              context.log.debug("FileSystemWatcher -> Checking if there are any subscribers to subfolders of {}...", folder)

              state.highestSubfoldersStillSubscribedToBelow(folder)
                .foreach { subfolder ⇒
                  context.log.debug("FileSystemWatcher -> Preparing to spawn a new FolderWatcher for {}...", subfolder)

                  // TODO Evaluate the need to handle timeouts when spawning
                  //  actors in the same way that we handle timeouts when
                  //  communicating with FileSystemWatcher from BrokerageNoteWatcher
                  // FIXME I need to handle request completion in this case.
                  //  I think I somewhat already am 'cause I believe the first
                  //  new actor to be spawned will complete it but, I need to make sure.
                  //  Also, ideally it would be completed when the last remaining actor is spawn
                  watch(subfolder)(requestId, context)
                }
            else
              context.log.debug("FileSystemWatcher -> It looks like there's still somebody else other than {} subscribed to {} so, this is it for {}.", subscriber, folder, requestId)

              Effect
                .persist[Event, State] {
                  val requestCompleted = RequestCompleted(
                    requestId,
                    RequestStatus.Fulfilled
                  )
                  context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", requestCompleted)

                  requestCompleted
                }
          }
          .thenReply(ackTo) { _ ⇒
            context.log.debug("FileSystemWatcher -> Replying back to the UnsubscribeFromBrokerageNotesOn request from {} with the requestId {}", ackTo, requestId)
            StatusReply.success(requestId)
          }

      case RemoveWatcherOf(path, requestId) =>
        if state.isNotWatching(path) then
          context.log.debug("FileSystemWatcher -> FolderWatcher actor that was suppose to watch {} has terminated. Probably, something went wrong on its initialization before it even got a change to start watching {}.", path, path)

          Effect
            .persist {
              RequestCompleted(requestId, RequestStatus.Failed {
                RuntimeException(s"Failure while spawning a child actor to watch $path. Make sure $path is a real folder.")
              })
            }
            .thenNoReply()
        else
          context.log.debug("FileSystemWatcher -> FolderWatcher actor that was watching {} has terminated.", path)

          Effect
            .persist {
              FolderWatcherTerminated(path)
            }
            .thenNoReply()

      case AckNewFile(file, ackTo) =>
        context.log.debug("FileSystemWatcher -> One of the FolderWatchers has found a new file: {}.", file)
        context.log.debug("FileSystemWatcher -> Checking if new-file ({}) notification from one of the FolderWatchers is not a duplicate...", file)

        if state.doesNotHave(file) then
          context.log.debug("FileSystemWatcher -> I can confirm the new-file ({}) notification from one of the FolderWatchers is not a duplicate.", file)

          Effect
            .persist[Event, State] {
              val folderWatcherFoundNewFile =
                FolderWatcherFoundNewFile(file)
              context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", folderWatcherFoundNewFile)

              folderWatcherFoundNewFile
            }
            .thenRun { state ⇒
              context.log.debug("FileSystemWatcher -> Checking if there is anybody interested in hearing about {}...", file)

              state.forAllPendingDeliveriesTo(file) { (subscriber, lifecycleEvent) ⇒
                context.log.debug("FileSystemWatcher -> Scheduling notification of {} regarding {} to {}...", lifecycleEvent, file, subscriber)

                timerScheduler.startSingleTimer(
                  "retry-deliver-installment-from-ack-new-file",
                  DeliverInstallment(
                    subscriber,
                    lifecycleEvent,
                    file
                  ),
                  500.millis
                )
              }
            }

        Effect.reply (ackTo) {
          StatusReply.ack()
        }

      case AckFileRemoved(file, ackTo) =>
        context.log.debug("FileSystemWatcher -> One of the FolderWatchers says the file {} has been deleted.", file)
        context.log.debug("FileSystemWatcher -> Checking if file-removed ({}) notification from one of the FolderWatchers is not a duplicate...", file)

        if state.has(file) then
          context.log.debug("FileSystemWatcher -> I can confirm the file-removed ({}) notification from one of the FolderWatchers is not a duplicate.", file)

          Effect
            .persist[Event, State] {
              val fileRemoved =
                FolderWatcherSaysFileRemoved(file)
              context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", fileRemoved)

              fileRemoved
            }
            .thenRun { state ⇒
              context.log.debug("FileSystemWatcher -> Checking if there is anybody interested in hearing about {}...", file)

              state.forAllPendingDeliveriesTo(file) { (subscriber, lifecycleEvent) ⇒
                context.log.debug("FileSystemWatcher -> Scheduling notification of {} regarding {} to {}...", lifecycleEvent, file, subscriber)

                timerScheduler.startSingleTimer(
                  "retry-deliver-installment-from-ack-file-removed",
                  DeliverInstallment(
                    subscriber,
                    lifecycleEvent,
                    file
                  ),
                  500.millis
                )
              }
            }

        Effect.reply(ackTo) {
          StatusReply.ack()
        }

      case DeliverInstallment(
        subscriber, lifecycleEvent, file
      ) ⇒
        context.log.debug("FileSystemWatcher -> Notifying {} about {} regarding {}.", subscriber, lifecycleEvent, file)

        context.askWithStatus(
          subscriber,
          lifecycleEvent
        ) {
          HandleInstallmentDeliverySignature(
            _,
            subscriber,
            lifecycleEvent,
            file
          )
        }

        Effect.noReply

      case HandleInstallmentDeliverySignature(
        signature, subscriber, lifecycleEvent, file
      ) ⇒ signature match
        case Success(done) ⇒
          context.log.debug("FileSystemWatcher -> {} acknowledged the successful receipt of {}.", subscriber, lifecycleEvent)

          Effect
            .persist {
              val lifecycleEventAcknowledged =
                InstallmentDeliverySigned(file, subscriber)
              context.log.debug("FileSystemWatcher -> Preparing to persist event: {}.", lifecycleEventAcknowledged)

              lifecycleEventAcknowledged
            }
            .thenNoReply()

        case Failure(exception) ⇒
          context.log.debug("FileSystemWatcher -> Notification of {} to {} has failed with {}. I will try again...", lifecycleEvent, subscriber, exception)

          timerScheduler.startSingleTimer(
            "retry-deliver-installment-from-file-system-watcher",
            DeliverInstallment(subscriber, lifecycleEvent, file),
            500.millis
          )

        Effect.noReply

      case GetStatus(optionalRequestId, replyTo) =>
        context.log.debug("FileSystemWatcher -> Preparing to get the status of requests received.")

        Effect.reply(replyTo) {
          optionalRequestId.map { requestId ⇒
            val requestStatus = state.statusOf(requestId)
            context.log.debug("FileSystemWatcher -> Sending requestStatus {} for requestId {} back to {}.", requestStatus, requestId, replyTo)
            requestStatus
          }.getOrElse {
            context.log.debug("FileSystemWatcher -> Sending the status of all requests received back to {}.", replyTo)
            state.requestStatuses
          }
        }

      case GetSubscriptions(replyTo) =>
        context.log.debug("FileSystemWatcher -> Preparing to get the subscription map.")

        Effect.reply(replyTo) {
          context.log.debug("FileSystemWatcher -> Sending the subscription map back to {}.", replyTo)
          state.subscriptions
        }

      case GetWatchedFolders(replyTo) =>
        context.log.debug("FileSystemWatcher -> Preparing to get the list of watched folders together with their watchers.")

        Effect.reply(replyTo) {
          context.log.debug("FileSystemWatcher -> Sending the list of watched folders together with their watchers back to {}.", replyTo)
          state.watchedFolders
        }

      case GetFiles(replyTo) =>
        context.log.debug("FileSystemWatcher -> Preparing to get the list of files reported by all watchers.")

        Effect.reply(replyTo) {
          context.log.debug("FileSystemWatcher -> Sending the list of files reported by all watchers back to {}.", replyTo)
          state.files
        }

      case GetPendingDeliveries(replyTo) =>
        context.log.debug("FileSystemWatcher -> Preparing to get the list of pending deliveries.")

        Effect.reply(replyTo) {
          context.log.debug("FileSystemWatcher -> Sending the list of pending deliveries back to {}.", replyTo)
          state.pendingDeliveries
        }

      // TODO Temporary Stop and Restart commands so that
      //  we can play with the actors. Remove it if this
      //  code gets promoted to production.
      case Restart(ackTo) =>
        context.log.debug("FileSystemWatcher -> I have been asked to restart. I'll be back in a moment...")

        throw new RuntimeException("I have been asked to restart. I'll be back in a moment...")

        Effect.reply(ackTo) {
          StatusReply.ack()
        }

      case Stop(ackTo) =>
        context.log.debug("FileSystemWatcher -> I have been asked to stop. Bye!")

        Effect
          .stop
          .thenReply(ackTo) { _ =>
            StatusReply.ack()
          }

      case ResendPendingRequestsAndDeliveries ⇒
        context.log.debug("FileSystemWatcher -> Resending pending requests and deliveries.")

        state.forAllPendingRequests { pendingRequest ⇒
          val folder      = pendingRequest.folder
          val onBehalfOf  = pendingRequest.onBehalfOf
          val requestId   = pendingRequest.requestId
          val sender      = pendingRequest.sender

          pendingRequest.requestType match
            case RequestType.SubscribeForChangesOn   ⇒
              context.log.debug("FileSystemWatcher -> Scheduling task to resend request to subscribe for changes on {} on behalf of {} as a continuation of {}.", folder, onBehalfOf, requestId)

              timerScheduler.startSingleTimer(
                "resend-subscribe-for-changes-on-from-file-system-watcher",
                SubscribeForChangesOn(
                  folder,
                  onBehalfOf,
                  Some(requestId),
                  sender
                ),
                500.millis
              )

            case RequestType.UnsubscribeFromChangesOn ⇒
              context.log.debug("FileSystemWatcher -> Scheduling task to resend request to unsubscribe from changes on {} on behalf of {} as a continuation of {}.", folder, onBehalfOf, requestId)

              timerScheduler.startSingleTimer(
                "resend-unsubscribe-from-changes-on-from-file-system-watcher",
                UnsubscribeFromChangesOn(
                  folder,
                  onBehalfOf,
                  Some(requestId),
                  sender
                ),
                500.millis
              )
        }

        state.forEachPendingDelivery { (file, subscriber, lifecycleEvent) ⇒
          context.log.debug("FileSystemWatcher -> Scheduling task to resend {} regarding {} to {}...", lifecycleEvent, file, subscriber)

          timerScheduler.startSingleTimer(
            "resend-deliver-installment-from-file-system-watcher",
            DeliverInstallment(
              subscriber,
              lifecycleEvent,
              file
            ),
            500.millis
          )
        }

        Effect.noReply

  private def watch(path: Path)(
    requestId : RequestId,
    context   : ActorContext[Command]
  )(using Timeout): Unit =
    //  Spawn and watch a new Folder actor
    context.log.debug("FileSystemWatcher -> Instantiating FolderWatcher actor for path {}...", path)

    val folderWatcher = FolderWatcher(path, requestId, context.self)
    context.log.debug("FileSystemWatcher -> Spawning FolderWatcher actor for path {}...", path)

    val folderWatcherRef = context.spawn(folderWatcher, s"FolderWatcher-${path.toString.replaceAll("[/\\\\ ]", "-")}")
    context.log.debug("FileSystemWatcher -> Watching FolderWatcher actor for path {}...", path)

    context.watchWith(folderWatcherRef, RemoveWatcherOf(path, requestId))

  private def handleEvent(
    state   : State,
    event   : Event,
    context : ActorContext[Command]
  ) = event match
    case RequestAccepted(
      requestId, requestType, folder, sender, onBehalfOf
    )  ⇒
      context.log.debug("FileSystemWatcher -> Processing event RequestAccepted({}, {}, {}).", requestId, requestType, folder)

      state.withRequest(
        Request(requestId, requestType, folder, sender, onBehalfOf)
      )

    case Subscribed(subscriber, path) ⇒
      context.log.debug("FileSystemWatcher -> Processing event Subscribed({}, {}).", subscriber, path)

      state.withSubscription(subscriber, path)

    case FolderWatcherConfirmedWatching(path, watcher)  ⇒
      context.log.debug("FileSystemWatcher -> Processing event FolderWatcherConfirmedWatching({}, {}).", path, watcher)

      state.watch(path, watcher)

    case Unsubscribed(subscriber, path) ⇒
      context.log.debug("FileSystemWatcher -> Processing event Unsubscribed({}, {}).", subscriber, path)

      state.withoutSubscription(subscriber, path)

    case FolderWatcherTerminated(path)                  ⇒
      context.log.debug("FileSystemWatcher -> Processing event FolderWatcherTerminated({}).", path)

      state.unwatch(path)

    case RequestCompleted(requestId, requestStatus)     ⇒
      context.log.debug("FileSystemWatcher -> Processing event RequestCompleted({}, {}, {}).", requestId, requestStatus)

      state.updateStatus(requestId, requestStatus)

    case FolderWatcherFoundNewFile(file)  ⇒
      context.log.debug("FileSystemWatcher -> Processing event FolderWatcherFoundNewFile({}).", file)

      state
        .withFile(file)
        .withPendingDeliveries(file, FileCreated(file, _))

    case FolderWatcherSaysFileRemoved(file)  ⇒
      context.log.debug("FileSystemWatcher -> Processing event FolderWatcherSaysFileRemoved({}).", file)

      state
        .withoutFile(file)
        .withPendingDeliveries(file, FileRemoved(file, _))

    case InstallmentDeliverySigned(file, subscriber) ⇒
      context.log.debug("FileSystemWatcher -> Processing event InstallmentDeliverySigned({}, {}).", file, subscriber)

      state.withDeliverySigned(file, subscriber)


  def apply()(using Timeout): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      Behaviors.supervise {
        Behaviors.supervise {
          context.log.debug("FileSystemWatcher -> Initializing behavior.")

          Behaviors.withTimers[Command] { timerScheduler ⇒
            EventSourcedBehavior
              .withEnforcedReplies[Command, Event, State](
                persistenceId   = PersistenceId.ofUniqueId("FileSystemWatcher"),
                emptyState      = State.empty,
                commandHandler  = (state, command)  => handleCommand(
                  state, command, context, timerScheduler
                ),
                eventHandler    = (state, event)    => handleEvent(
                  state, event, context
                )
              )
              // FIXME This probably won't need snapshotting
              .withRetention {
                RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 3)
              }
              // TODO In case this ever changes:
              // What the configuration below actually means is: What should be done to
              // the event sourced actor when it fails with a JournalFailureException.
              // This exception seems to be thrown both when a persist fails (for example,
              // the database is unreachable) and when replaying events fail. There's no
              // way to handle them separately. I want to stop the actor if it fails to
              // recover because it can't replay the events due to serialization problems.
              // There's nothing I can do in this case other than stop and fix the problem
              // programmatically. On the other hand, I would like to retry if a persist
              // failed due to the database being momentarily unreachable but, apparently
              // I can't handle these very different situations in different ways only
              // because the same exception is thrown at both situations.
              //.onPersistFailure {
              //  SupervisorStrategy.restartWithBackoff(200.millis, 5.seconds, 0.1)
              //}
              .receiveSignal {
                case (_, signal: PreRestart) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: PostStop) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: RecoveryFailed) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: RecoveryCompleted) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                  context.log.debug("FileSystemWatcher -> Preparing to resend pending requests and deliveries...")

                  Behaviors.withTimers { timers =>
                    timers.startSingleTimer(
                      "file-system-watcher-recovery-completed",
                      ResendPendingRequestsAndDeliveries,
                      10.millis
                    )
                    Behaviors.same
                  }
                case (_, signal: ChildFailed) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: Terminated) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: MessageAdaptionFailure) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: SnapshotFailed) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: SnapshotCompleted) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: DeleteSnapshotsFailed) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: DeleteSnapshotsCompleted) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: DeleteEventsFailed) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: DeleteEventsCompleted) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: EventSourcedSignal) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
                case (_, signal: Signal) =>
                  context.log.debug("FileSystemWatcher -> Signal received {}", signal)
              }
          }
        }.onFailure[ActorInitializationException] {
          SupervisorStrategy.stop
        }
      }.onFailure {
        SupervisorStrategy.restart
      }
    }