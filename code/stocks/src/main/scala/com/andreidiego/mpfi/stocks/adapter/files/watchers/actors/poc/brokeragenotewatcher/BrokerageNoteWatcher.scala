package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.brokeragenotewatcher

import java.nio.file.Path
import java.util.UUID
import org.slf4j.{Logger, LoggerFactory}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import scala.annotation.experimental
import scala.io.StdIn
import scala.sys.addShutdownHook
import scala.util.{Failure, Success, Try}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import akka.Done
import akka.actor.ActorInitializationException
import akka.pattern.StatusReply
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PreRestart}
import akka.actor.typed.{Props, SpawnProtocol, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import com.andreidiego.mpfi.stocks.adapter.files
import files.readers.pdf.poc.PDFBrokerageNotePath
import files.watchers.actors.poc.{CborSerializable, RequestId}
import files.watchers.actors.poc.filewatcher.FileSystemWatcher

object BrokerageNoteWatcher:

  sealed trait Protocol extends CborSerializable

  @JsonSerialize(`using`    = classOf[RequestTypeSerializer])
  @JsonDeserialize(`using`  = classOf[RequestTypeDeserializer])
  enum RequestType extends Protocol:
    case SubscribeForPDFBrokerageNoteLifecycleEventsOn, UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn

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
    onBehalfOf  : ActorRef[PDFBrokerageNoteLifecycle]
  )

  sealed trait Command extends Protocol
  final case class SubscribeForPDFBrokerageNoteLifecycleEventsOn(
    folder            : Path,
    subscriber        : ActorRef[PDFBrokerageNoteLifecycle],
    optionalRequestId : Option[RequestId],
    ackTo             : ActorRef[StatusReply[RequestId]]
  )                                                                                     extends Command
  private final case class SubscribeForChangesOn(
    folder                : Path,
    requestId             : RequestId,
    shouldCompleteRequest : Boolean
  )                                                                                     extends Command
  private case class HandleResponseToSubscribeForChangesOn(
    response              : Try[RequestId],
    requestId             : RequestId,
    folder                : Path,
    shouldCompleteRequest : Boolean
  )                                                                                     extends Command
  final case class UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn(
    folder            : Path,
    subscriber        : ActorRef[PDFBrokerageNoteLifecycle],
    optionalRequestId : Option[RequestId],
    ackTo             : ActorRef[StatusReply[RequestId]]
  )                                                                                     extends Command
  private final case class UnsubscribeFromChangesOn(
    folder                : Path,
    requestId             : RequestId,
    shouldCompleteRequest : Boolean
  )                                                                                     extends Command
  private case class HandleResponseToUnsubscribeFromChangesOn(
    response              : Try[RequestId],
    requestId             : RequestId,
    folder                : Path,
    shouldCompleteRequest : Boolean
  )                                                                                     extends Command
  private final case class AdaptLifecycleResponse(
    fileLifecycleEvent: FileSystemWatcher.FileLifecycleEvent
  )                                                                                     extends Command
  @experimental private final case class DeliverInstallment[F[_]](
    subscriber          : ActorRef[PDFBrokerageNoteLifecycle],
    lifecycleEvent      : ActorRef[StatusReply[Done]] ⇒ PDFBrokerageNoteLifecycle,
    pdfBrokerageNotePath: PDFBrokerageNotePath[F]
  )                                                                                     extends Command
  @experimental private case class HandleInstallmentDeliverySignature[F[_]](
    signature           : Try[Done],
    subscriber          : ActorRef[PDFBrokerageNoteLifecycle],
    lifecycleEvent      : ActorRef[StatusReply[Done]] ⇒ PDFBrokerageNoteLifecycle,
    pdfBrokerageNotePath: PDFBrokerageNotePath[F]
  )                                                                                     extends Command
  final case class GetStatuses(replyTo: ActorRef[Map[Request, RequestStatus]])          extends Command
  final case class GetSubscriptions(
    replyTo: ActorRef[Map[Path, Seq[ActorRef[PDFBrokerageNoteLifecycle]]]]
  )                                                                                     extends Command
  final case class GetWatchedFolders(
    replyTo: ActorRef[Seq[Path]]
  )                                                                                     extends Command
  @experimental final case class GetBrokerageNotes[F[_]](
    replyTo: ActorRef[Seq[PDFBrokerageNotePath[F]]]
  )                                                                                     extends Command
  @experimental final case class GetPendingDeliveries[F[_]](
    replyTo: ActorRef[Iterable[(
      PDFBrokerageNotePath[F],
      ActorRef[PDFBrokerageNoteLifecycle],
      ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
    )]]
  )                                                                                     extends Command
  case class Restart(ackTo: ActorRef[StatusReply[Done]])                                extends Command
  case class Stop(ackTo: ActorRef[StatusReply[Done]])                                   extends Command
  private case object ResendPendingRequestsAndDeliveries                                extends Command

  private sealed trait Event[F[_]] extends Protocol
  private final case class RequestAccepted[F[_]](
    requestId   : RequestId,
    requestType : RequestType,
    folder      : Path,
    sender      : ActorRef[StatusReply[RequestId]],
    onBehalfOf  : ActorRef[PDFBrokerageNoteLifecycle]
  )                                                                                     extends Event[F]
  private final case class Subscribed[F[_]](
    subscriber: ActorRef[PDFBrokerageNoteLifecycle],
    folder: Path
  )                                                                                     extends Event[F]
  private final case class FileSystemWatcherConfirmedSubscriptionTo[F[_]](folder: Path) extends Event[F]
  private final case class Unsubscribed[F[_]](
    subscriber: ActorRef[PDFBrokerageNoteLifecycle],
    folder    : Path
  )                                                                                     extends Event[F]
  private final case class FileSystemWatcherConfirmedCancelationOfSubscriptionTo[F[_]](
    folder: Path
  )                                                                                     extends Event[F]
  private final case class RequestCompleted[F[_]](
    requestId     : RequestId,
    requestStatus : RequestStatus
  )                                                                                     extends Event[F]
  @experimental private final case class NewPDFBrokerageNoteDetected[F[_]](
    pdfBrokerageNotePath: PDFBrokerageNotePath[F]
  )                                                                                     extends Event[F]
  @experimental private final case class PDFBrokerageNoteGone[F[_]](
    pdfBrokerageNotePath: PDFBrokerageNotePath[F]
  )                                                                                     extends Event[F]
  @experimental private final case class InstallmentDeliverySigned[F[_]](
    pdfBrokerageNotePath: PDFBrokerageNotePath[F],
    subscriber          : ActorRef[PDFBrokerageNoteLifecycle]
  )                                                                                     extends Event[F]

  sealed trait Response extends Protocol
  // TODO Add a supertype for messages that are 'acknowledgeable' (have a ackTo: ActorRef[StatusReply[Done]])???
  sealed trait PDFBrokerageNoteLifecycle extends Response
  @experimental final case class PDFBrokerageNoteCreated[F[_]](
    pdfBrokerageNotePath: PDFBrokerageNotePath[F],
    ackTo               : ActorRef[StatusReply[Done]]
  )                                                                   extends PDFBrokerageNoteLifecycle
  @experimental final case class PDFBrokerageNoteRemoved[F[_]](
    pdfBrokerageNotePath: PDFBrokerageNotePath[F],
    ackTo               : ActorRef[StatusReply[Done]]
  )                                                                   extends PDFBrokerageNoteLifecycle

  @experimental final case class State[F[_]](
    clientRequests    : Map[Request, RequestStatus],
    subscriptions     : Map[Path, Seq[ActorRef[PDFBrokerageNoteLifecycle]]],
    watchedFolders    : Seq[Path],
    files             : Seq[PDFBrokerageNotePath[F]],
    pendingDeliveries : Iterable[(
      PDFBrokerageNotePath[F],
      ActorRef[PDFBrokerageNoteLifecycle],
      ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
    )]
  ) extends Protocol:
    def withRequest(request: Request): State[F] = State(
      clientRequests + (request → RequestStatus.Accepted),
      subscriptions,
      watchedFolders,
      files,
      pendingDeliveries
    )
    def update(requestId: RequestId, requestStatus: RequestStatus): State[F] =
      val request = clientRequests
        .find((request, _) ⇒ request.requestId == requestId)
        .map(_._1)
        .getOrElse {
          throw new RuntimeException(
            s"Failed to update the status of request $requestId to $requestStatus. $requestId could not be found on BrokerageNoteWatcher state."
          )
        }
      State(
        clientRequests + (request → requestStatus),
        subscriptions,
        watchedFolders,
        files,
        pendingDeliveries
      )
    def forAllPendingRequests(execute: Request ⇒ Unit): Unit = clientRequests
      .filter(_._2 == RequestStatus.Accepted)
      .foreach((request, _) ⇒ execute(request))

    def withSubscription(
      subscriber: ActorRef[PDFBrokerageNoteLifecycle],
      folder    : Path
    ): State[F] = State(
      clientRequests,
      subscriptions + (folder → (subscriber +: subscriptions.getOrElse(folder, Seq.empty))),
      watchedFolders,
      files,
      pendingDeliveries
    )
    def withoutSubscription(
      subscriber: ActorRef[PDFBrokerageNoteLifecycle],
      folder    : Path
    ): State[F] = State(
      clientRequests,
      subscriptions.updatedWith(folder) { _.map {
        _.filterNot(_ == subscriber)
      }},
      watchedFolders,
      files,
      pendingDeliveries
    )
    def hasAnybodyElseInterestedOn(folder: Path): Boolean = subscriptions
      .exists((watchedFolder, _) ⇒ watchedFolder.startsWith(folder))
    def hasNobodyElseInterestedOn(folder: Path) : Boolean = !hasAnybodyElseInterestedOn(folder)
    def noSubscriberLeft(to: Path)(
      otherThan: ActorRef[PDFBrokerageNoteLifecycle]
    )                                           : Boolean = subscriptions
      .get(to)
      .map(_.filterNot(_ == otherThan).isEmpty)
      .getOrElse(true)
    def highestSubfoldersStillSubscribedToBelow(folder: Path): Iterable[Path] = subscriptions
      .filter((watchedFolder, _)  ⇒ watchedFolder.startsWith(folder) && watchedFolder != folder)
      .groupBy((watchedFolder, _) ⇒ watchedFolder.getNameCount)
      .toSeq
      .minBy(_._1)
      ._2
      .keys

    def watch(folder: Path): State[F] = State(
      clientRequests,
      subscriptions,
      folder +: watchedFolders.filterNot(_.startsWith(folder)),
      files,
      pendingDeliveries
    )
    def unwatch(folder: Path): State[F] = State(
      clientRequests, subscriptions, watchedFolders.filterNot(_ == folder), files, pendingDeliveries
    )
    def isAlreadyWatching(folder: Path)   : Boolean = watchedFolders
      .exists(watchedFolder ⇒ folder.startsWith(watchedFolder))
    def isNotAlreadyWatching(folder: Path): Boolean = !isAlreadyWatching(folder)

    def withFile(file: PDFBrokerageNotePath[F])    : State[F] = State(
      clientRequests, subscriptions, watchedFolders, file +: files, pendingDeliveries
    )
    def withoutFile(file: PDFBrokerageNotePath[F]) : State[F] = State(
      clientRequests, subscriptions, watchedFolders, files.filterNot(_ == file), pendingDeliveries
    )
    def has[F[_]](pdfBrokerageNotePath: PDFBrokerageNotePath[F]): Boolean =
      files.contains(pdfBrokerageNotePath)
    def doesNotHave[F[_]](pdfBrokerageNotePath: PDFBrokerageNotePath[F]): Boolean =
      !has(pdfBrokerageNotePath)

    def withPendingDeliveries(
      file          : PDFBrokerageNotePath[F],
      lifecycleEvent: ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
    ): State[F] =
      val subscribers: Seq[ActorRef[PDFBrokerageNoteLifecycle]] = subscriptions
        .filter((folder, _) ⇒ Path.of(file.path).startsWith(folder))
        .values
        .flatten
        .toSeq

      State(
        clientRequests,
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
      pdfBrokerageNotePath: PDFBrokerageNotePath[F],
      subscriber          : ActorRef[PDFBrokerageNoteLifecycle]
    ): State[F] = State(
      clientRequests,
      subscriptions,
      watchedFolders,
      files,
      pendingDeliveries.filterNot { (file, sendTo, _) =>
        file == pdfBrokerageNotePath && sendTo == subscriber
      }
    )
    def forAllPendingDeliveriesTo(
      pdfBrokerageNotePath: PDFBrokerageNotePath[F]
    )(
      execute: (
        ActorRef[PDFBrokerageNoteLifecycle],
        ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
      ) => Unit
    ): Unit = pendingDeliveries
      .filter { (file, _, _) =>
        file == pdfBrokerageNotePath
      }
      .foreach { (_, sendTo, lifecycleEvent) =>
        execute(sendTo, lifecycleEvent)
      }
    def forEachPendingDelivery(
      execute: (
        PDFBrokerageNotePath[F],
        ActorRef[PDFBrokerageNoteLifecycle],
        ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
      ) => Unit
    ): Unit = pendingDeliveries
      .foreach { (file, subscriber, lifecycleEvent) =>
        execute(file, subscriber, lifecycleEvent)
      }

  object State:
    def empty[F[_]]: State[F] = State(Map.empty, Map.empty, Seq.empty, Seq.empty, Iterable.empty)

  @experimental private def handleCommand[F[_]](
    state                       : State[F],
    command                     : Command,
    fileSystemWatcher           : ActorRef[FileSystemWatcher.Command],
    context                     : ActorContext[Command],
    timerScheduler              : TimerScheduler[Command],
    fileLifecycleResponseMapper : ActorRef[FileSystemWatcher.FileLifecycleEvent]
  )(using Timeout): ReplyEffect[Event[F], State[F]] =
    context.log.debug("BrokerageNoteWatcher -> Received command {}.", command)

    command match
      case SubscribeForPDFBrokerageNoteLifecycleEventsOn(
        folder, subscriber, optionalRequestId, ackTo
      ) ⇒
        context.log.debug("BrokerageNoteWatcher -> {} wants to subscribe {} for brokerage notes from {}", ackTo, subscriber, folder)

        val requestId = optionalRequestId
          .getOrElse(RequestId(UUID.randomUUID()))
        context.log.debug("BrokerageNoteWatcher -> {}.", requestId)

        Effect
          .persist[Event[F], State[F]] {
            val requestAccepted = RequestAccepted[F](
              requestId,
              RequestType.SubscribeForPDFBrokerageNoteLifecycleEventsOn,
              folder,
              ackTo,
              subscriber
            )
            context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestAccepted)

            val subscribed = Subscribed[F](subscriber, folder)
            context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", subscribed)

            Seq(requestAccepted, subscribed)
          }
          .thenRun { state ⇒
            context.log.debug("BrokerageNoteWatcher -> Checking if I'm already watching {}.", folder)

            if state.isNotAlreadyWatching(folder) then
              context.log.debug("BrokerageNoteWatcher -> It looks like I'm not watching {} so, I'll ask FileSystemWatcher.", folder)

              timerScheduler.startSingleTimer(
                "retry-subscribe-for-changes-on-from-subscribe",
                SubscribeForChangesOn(folder, requestId, true),
                500.millis
              )
            else
              context.log.debug("BrokerageNoteWatcher -> It looks like I'm already watching {} so, this is it for {}.", folder, requestId)

              Effect
                .persist[Event[F], State[F]] {
                  val requestCompleted = RequestCompleted[F](
                    requestId,
                    RequestStatus.Fulfilled
                  )
                  context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestCompleted)

                  requestCompleted
                }
          }
          .thenReply(ackTo) { _ ⇒
            context.log.debug("BrokerageNoteWatcher -> Replying back to the SubscribeForBrokerageNotesOn request from {} with the requestId {}", ackTo, requestId)
            StatusReply.success(requestId)
          }

      case SubscribeForChangesOn(
        folder, requestId, shouldCompleteRequest
      ) ⇒
        context.log.debug("BrokerageNoteWatcher -> Subscribing to {} for changes on folder {} as part of {}.", fileSystemWatcher, folder, requestId)

        context.askWithStatus[FileSystemWatcher.Command, RequestId](
          fileSystemWatcher,
          FileSystemWatcher.SubscribeForChangesOn(
            folder,
            fileLifecycleResponseMapper,
            Some(requestId),
            _
          )
        ) {
          HandleResponseToSubscribeForChangesOn(
            _,
            requestId,
            folder,
            shouldCompleteRequest
          )
        }

        Effect.noReply

      case HandleResponseToSubscribeForChangesOn(
        response, requestId, folder, shouldCompleteRequest
      ) ⇒ response match
        case Success(requestId) ⇒
          context.log.debug("BrokerageNoteWatcher -> Request {} to {} to subscribe for changes on folder {} succeeded. I'm now subscribed to {}.", requestId, fileSystemWatcher, folder, folder)

          Effect
            .persist {
              val subscriptionConfirmed =
                FileSystemWatcherConfirmedSubscriptionTo(folder)
              context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", subscriptionConfirmed)

              if shouldCompleteRequest then
                val requestCompleted =
                  RequestCompleted(requestId, RequestStatus.Fulfilled)
                context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestCompleted)

                Seq(subscriptionConfirmed, requestCompleted)
              else subscriptionConfirmed
            }
            .thenNoReply()

        case Failure(exception) ⇒
          context.log.debug("BrokerageNoteWatcher -> Request {} to {} to subscribe for changes on folder {} has failed with {}. I will try again...", requestId, fileSystemWatcher, folder, exception)

          timerScheduler.startSingleTimer(
            "retry-subscribe-for-changes-on",
            SubscribeForChangesOn(
              folder,
              requestId,
              shouldCompleteRequest
            ),
            500.millis
          )

        Effect.noReply

      case UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn(
        folder, subscriber, optionalRequestId, ackTo
      ) ⇒
        context.log.debug("BrokerageNoteWatcher -> {} wants to unsubscribe {} from brokerage notes from {}", ackTo, subscriber, folder)

        val requestId = optionalRequestId
          .getOrElse(RequestId(UUID.randomUUID()))
        context.log.debug("BrokerageNoteWatcher -> {}.", requestId)

        Effect
          .persist[Event[F], State[F]] {
            val requestAccepted = RequestAccepted[F](
              requestId,
              RequestType.UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn,
              folder,
              ackTo,
              subscriber
            )
            context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestAccepted)

            val unsubscribed = Unsubscribed[F](subscriber, folder)
            context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", unsubscribed)

            Seq(requestAccepted, unsubscribed)
          }
          .thenRun { state ⇒
            context.log.debug("BrokerageNoteWatcher -> Checking if there's still somebody interested in brokerage notes from {}...", folder)

              if state.noSubscriberLeft(to = folder)(otherThan = subscriber) then
                context.log.debug("BrokerageNoteWatcher -> It looks like nobody else other than {} is subscribed to {} so, I'll unsubscribe myself from {} on FileSystemWatcher.", subscriber, folder, folder)

                val newSubscribers =
                  state.highestSubfoldersStillSubscribedToBelow(folder)

                timerScheduler.startSingleTimer(
                  "retry-unsubscribe-from-changes-on-from-unsubscribe",
                  UnsubscribeFromChangesOn(
                    folder,
                    requestId,
                    newSubscribers.isEmpty
                  ),
                  500.millis
                )
                context.log.debug("BrokerageNoteWatcher -> Checking if there are any subscribers to subfolders of {}...", folder)

                newSubscribers.foreach { subfolder ⇒
                  context.log.debug("BrokerageNoteWatcher -> Scheduling task to subscribe for changes on {} with FileSystemWatcher...", subfolder)

                  timerScheduler.startSingleTimer(
                    "retry-subscribe-for-changes-on-from-unsubscribe",
                    // TODO Ideally, only the last message should complete the request but, for now, this should do it.
                    SubscribeForChangesOn(subfolder, requestId, true),
                    500.millis
                  )
               }
              else
                context.log.debug("BrokerageNoteWatcher -> It looks like there's still somebody else other than {} subscribed to {} so, this is it for {}.", subscriber, folder, requestId)

                Effect
                  .persist[Event[F], State[F]] {
                    val requestCompleted = RequestCompleted[F](
                      requestId,
                      RequestStatus.Fulfilled
                    )
                    context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestCompleted)

                    requestCompleted
                  }
          }
          .thenReply(ackTo) { _ ⇒
            context.log.debug("BrokerageNoteWatcher -> Replying back to the UnsubscribeFromBrokerageNotesOn request from {} with the requestId {}", ackTo, requestId)
            StatusReply.success(requestId)
          }

      case UnsubscribeFromChangesOn(
        folder, requestId, shouldCompleteRequest
      ) ⇒
        context.log.debug("BrokerageNoteWatcher -> Asking {} to cancel subscription to folder {} as part of {}.", fileSystemWatcher, folder, requestId)

        context.askWithStatus[FileSystemWatcher.Command, RequestId](
          fileSystemWatcher,
          FileSystemWatcher.UnsubscribeFromChangesOn(
            folder,
            fileLifecycleResponseMapper,
            Some(requestId),
            _
          )
        ) {
          HandleResponseToUnsubscribeFromChangesOn(
            _,
            requestId,
            folder,
            shouldCompleteRequest
          )
        }

        Effect.noReply

      case HandleResponseToUnsubscribeFromChangesOn(
        response, requestId, folder, shouldCompleteRequest
      ) ⇒ response match
        case Success(requestId) ⇒
          context.log.debug("BrokerageNoteWatcher -> Request {} to {} to unsubscribe from changes on {} succeeded.", requestId, fileSystemWatcher, folder)

          Effect
            .persist {
              val subscriptionCancelationConfirmed =
                FileSystemWatcherConfirmedCancelationOfSubscriptionTo(folder)
              context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", subscriptionCancelationConfirmed)

              if shouldCompleteRequest then
                val requestCompleted =
                  RequestCompleted(requestId, RequestStatus.Fulfilled)
                context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", requestCompleted)

                Seq(subscriptionCancelationConfirmed, requestCompleted)

              else subscriptionCancelationConfirmed
            }
            .thenNoReply()

        case Failure(exception) ⇒
          context.log.debug("BrokerageNoteWatcher -> Request {} to {} to unsubscribe from changes on {} has failed with {}. I will try again...", requestId, fileSystemWatcher, folder, exception)

          timerScheduler.startSingleTimer(
            "retry-unsubscribe-from-changes-on",
            UnsubscribeFromChangesOn(folder, requestId, shouldCompleteRequest),
            500.millis
          )

        Effect.noReply

      case AdaptLifecycleResponse(fileLifecycleEvent) ⇒ fileLifecycleEvent match

        case FileSystemWatcher.FileCreated(file, ackTo) ⇒
          context.log.debug("BrokerageNoteWatcher -> I just heard from FileSystemWatcher that a new file was created: {}.", file)

          import unsafeExceptions.canThrowAny

          Try {
            context.log.debug("BrokerageNoteWatcher -> I'm checking if the new file looks like a PDFBrokerageNote: {}.", file)

            PDFBrokerageNotePath.from[F](file.toString)
          } match

            case Success(pdfBrokerageNotePath)     ⇒
              context.log.debug("BrokerageNoteWatcher -> I confirm that the new file in fact looks like a PDFBrokerageNote: {}.", pdfBrokerageNotePath)

              if state.doesNotHave(pdfBrokerageNotePath) then
                Effect
                  .persist[Event[F], State[F]] {
                    val pdfBrokerageNoteDetected =
                      NewPDFBrokerageNoteDetected[F](pdfBrokerageNotePath)
                    context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", pdfBrokerageNoteDetected)

                    pdfBrokerageNoteDetected
                  }
                  .thenRun { state ⇒
                    context.log.debug("BrokerageNoteWatcher -> Checking if there is anybody interested in hearing about {}...", pdfBrokerageNotePath)

                    state.forAllPendingDeliveriesTo(pdfBrokerageNotePath) { (subscriber, lifecycleEvent) ⇒
                      context.log.debug("BrokerageNoteWatcher -> Scheduling notification of {} regarding {} to {}...", lifecycleEvent, pdfBrokerageNotePath, subscriber)

                      timerScheduler.startSingleTimer(
                        "retry-deliver-installment-from-file-created",
                        DeliverInstallment(
                          subscriber,
                          lifecycleEvent,
                          pdfBrokerageNotePath
                        ),
                        500.millis
                      )
                    }
                  }

            case Failure(exception) ⇒
              context.log.debug("BrokerageNoteWatcher -> The new file does not look like a PDFBrokerageNote so, I'm ignoring it. Failure: {}.", exception)

          Effect.reply(ackTo) {
            StatusReply.ack()
          }

        case FileSystemWatcher.FileRemoved(file, ackTo) ⇒
          context.log.debug("BrokerageNoteWatcher -> I just heard from FileSystemWatcher that a file was removed: {}.", file)

          import unsafeExceptions.canThrowAny

          Try {
            context.log.debug("BrokerageNoteWatcher -> I'm checking if the file looked like a PDFBrokerageNote: {}.", file)

            PDFBrokerageNotePath.from[F](file.toString)
          } match

            case Success(pdfBrokerageNotePath) ⇒
              context.log.debug("BrokerageNoteWatcher -> I confirm that the file looked like a PDFBrokerageNotePath: {}.", pdfBrokerageNotePath)

              if state.has(pdfBrokerageNotePath) then
                Effect
                  .persist[Event[F], State[F]] {
                    val pdfBrokerageNoteGone =
                      PDFBrokerageNoteGone[F](pdfBrokerageNotePath)
                    context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", pdfBrokerageNoteGone)

                    pdfBrokerageNoteGone
                  }
                  .thenRun { state ⇒
                    context.log.debug("BrokerageNoteWatcher -> Checking if there is anybody interested in hearing about {}...", pdfBrokerageNotePath)

                    state.forAllPendingDeliveriesTo(pdfBrokerageNotePath) { (subscriber, lifecycleEvent) ⇒
                      context.log.debug("BrokerageNoteWatcher -> Scheduling notification of {} regarding {} to {}...", lifecycleEvent, pdfBrokerageNotePath, subscriber)

                      timerScheduler.startSingleTimer(
                        "retry-deliver-installment-from-file-removed",
                        DeliverInstallment(
                          subscriber,
                          lifecycleEvent,
                          pdfBrokerageNotePath
                        ),
                        500.millis
                      )
                    }
                  }

            case Failure(exception) ⇒
              context.log.debug("BrokerageNoteWatcher -> The file didn't look like a PDFBrokerageNotePath so, I'm ignoring it. Failure: {}.", exception)

          Effect.reply(ackTo) {
            StatusReply.ack()
          }

      case DeliverInstallment(
        subscriber, lifecycleEvent, pdfBrokerageNotePath
      ) ⇒
        context.log.debug("BrokerageNoteWatcher -> Notifying {} about {}.", subscriber, lifecycleEvent)

        context.askWithStatus(
          subscriber,
          lifecycleEvent
        ) {
          HandleInstallmentDeliverySignature(
            _,
            subscriber,
            lifecycleEvent,
            pdfBrokerageNotePath
          )
        }

        Effect.noReply

      case HandleInstallmentDeliverySignature(
        signature, subscriber, lifecycleEvent, pdfBrokerageNotePath
      ) ⇒ signature match

        case Success(done) ⇒
          context.log.debug("BrokerageNoteWatcher -> {} acknowledged the successful receipt of {}.", subscriber, lifecycleEvent)

          Effect
            .persist {
              val lifecycleEventAcknowledged =
                InstallmentDeliverySigned(pdfBrokerageNotePath, subscriber)
              context.log.debug("BrokerageNoteWatcher -> Preparing to persist event: {}.", lifecycleEventAcknowledged)

              lifecycleEventAcknowledged
            }
            .thenNoReply()

        case Failure(exception) ⇒
          context.log.debug("BrokerageNoteWatcher -> Notification of {} to {} has failed with {}. I will try again...", lifecycleEvent, subscriber, exception)

          timerScheduler.startSingleTimer(
            "retry-deliver-installment",
            DeliverInstallment(subscriber, lifecycleEvent, pdfBrokerageNotePath),
            500.millis
          )

        Effect.noReply

      case GetStatuses(replyTo) =>
        context.log.debug("BrokerageNoteWatcher -> Preparing to get the status of requests received.")

        Effect.reply(replyTo) {
          context.log.debug("BrokerageNoteWatcher -> Sending the status of all requests received back to {}.", replyTo)
          state.clientRequests
        }

      case GetSubscriptions(replyTo) =>
        context.log.debug("BrokerageNoteWatcher -> Preparing to get the subscription map.")

        Effect.reply(replyTo) {
          context.log.debug("BrokerageNoteWatcher -> Sending the subscription map back to {}.", replyTo)
          state.subscriptions
        }

      case GetWatchedFolders(replyTo) =>
        context.log.debug("BrokerageNoteWatcher -> Preparing to get the list of folders currently being watched for brokerage notes.")

        Effect.reply(replyTo) {
          context.log.debug("BrokerageNoteWatcher -> Sending the list of folders currently being watched for brokerage notes back to {}.", replyTo)
          state.watchedFolders
        }

      case GetBrokerageNotes[F](replyTo) =>
        context.log.debug("BrokerageNoteWatcher -> Preparing to get the list of all brokerage notes I know about.")

        Effect.reply(replyTo) {
          context.log.debug("BrokerageNoteWatcher -> Sending the list of all brokerage notes I know about back to {}.", replyTo)
          state.files
        }

      case GetPendingDeliveries[F](replyTo) =>
        context.log.debug("BrokerageNoteWatcher -> Preparing to get the list of pending deliveries.")

        Effect.reply(replyTo) {
          context.log.debug("BrokerageNoteWatcher -> Sending the list of pending deliveries back to {}.", replyTo)
          state.pendingDeliveries
        }

      // TODO Temporaty Stop and Restart commands so that
      //  we can play with the actors. Remove it if this
      //  code gets promoted to production.
      case Restart(ackTo) =>
        context.log.debug("BrokerageNoteWatcher -> I have been asked to restart. I'll be back in a moment...")

        throw new RuntimeException("I have been asked to restart. I'll be back in a moment...")

        Effect.reply(ackTo) {
          StatusReply.ack()
        }

      case Stop(ackTo) =>
        context.log.debug("BrokerageNoteWatcher -> I have been asked to stop. Bye!")

        Effect
          .stop
          .thenReply(ackTo) { _ =>
            StatusReply.ack()
          }

      case ResendPendingRequestsAndDeliveries ⇒

        state.forAllPendingRequests { pendingRequest ⇒
          val folder      = pendingRequest.folder
          val onBehalfOf  = pendingRequest.onBehalfOf
          val requestId   = pendingRequest.requestId
          val sender      = pendingRequest.sender

          pendingRequest.requestType match

            case RequestType.SubscribeForPDFBrokerageNoteLifecycleEventsOn   ⇒
              context.log.debug("BrokerageNoteWatcher -> Scheduling task to resend request to subscribe for PDF brokerage note lifecycle events on {} on behalf of {} as a continuation of {}.", folder, onBehalfOf, requestId)

              timerScheduler.startSingleTimer(
                "retry-subscribe-for-pdf-brokerage-note-lifecycle-events-on",
                SubscribeForPDFBrokerageNoteLifecycleEventsOn(
                  folder,
                  onBehalfOf,
                  Some(requestId),
                  sender
                ),
                500.millis
              )

            case RequestType.UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn ⇒
              context.log.debug("BrokerageNoteWatcher -> Scheduling task to resend request to unsubscribe from PDF brokerage note lifecycle events on {} on behalf of {} as a continuation of {}.", folder, onBehalfOf, requestId)

              timerScheduler.startSingleTimer(
                "retry-unsubscribe-from-pdf-brokerage-note-lifecycle-events-on",
                UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn(
                  folder,
                  onBehalfOf,
                  Some(requestId),
                  sender
                ),
                500.millis
              )
        }

        state.forEachPendingDelivery { (pdfBrokerageNotePath, subscriber, lifecycleEvent) ⇒
          context.log.debug("BrokerageNoteWatcher -> Scheduling task to resend {} regarding {} to {}...", lifecycleEvent, pdfBrokerageNotePath, subscriber)

          timerScheduler.startSingleTimer(
            "retry-deliver-installment-from-resend-pending-requests-and-deliveries",
            DeliverInstallment(
              subscriber,
              lifecycleEvent,
              pdfBrokerageNotePath
            ),
            500.millis
          )
        }

        Effect.noReply

  @experimental private def handleEvent[F[_]](
    state: State[F],
    event: Event[F],
    context: ActorContext[Command]
  ): State[F] =
    event match
      case RequestAccepted(requestId, requestType, folder, sender, onBehalfOf) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event RequestAccepted({}, {}, {}, {}, {}).", requestId, requestType, folder, sender, onBehalfOf)

        state.withRequest(Request(requestId, requestType, folder, sender, onBehalfOf))

      case Subscribed(subscriber, folder) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event Subscribed({}, {}).", subscriber, folder)

        state.withSubscription(subscriber, folder)

      case FileSystemWatcherConfirmedSubscriptionTo(folder) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event FileSystemWatcherConfirmedSubscriptionTo({}).", folder)

        state.watch(folder)

      case Unsubscribed(subscriber, folder) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event Unsubscribed({}, {}).", subscriber, folder)

        state.withoutSubscription(subscriber, folder)

      case FileSystemWatcherConfirmedCancelationOfSubscriptionTo(folder) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event FileSystemWatcherConfirmedCancelationOfSubscriptionTo({}).", folder)

        state.unwatch(folder)

      case RequestCompleted(requestId, requestStatus) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event RequestCompleted({}, {}).", requestId, requestStatus)

        state.update(requestId, requestStatus)

      case NewPDFBrokerageNoteDetected(pdfBrokerageNotePath) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event NewPDFBrokerageNoteDetected({}).", pdfBrokerageNotePath)

        state
          .withFile(pdfBrokerageNotePath)
          .withPendingDeliveries(pdfBrokerageNotePath, PDFBrokerageNoteCreated(pdfBrokerageNotePath, _))

      case PDFBrokerageNoteGone(pdfBrokerageNotePath) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event PDFBrokerageNoteGone({}).", pdfBrokerageNotePath)

        state
          .withoutFile(pdfBrokerageNotePath)
          .withPendingDeliveries(pdfBrokerageNotePath, PDFBrokerageNoteRemoved(pdfBrokerageNotePath, _))

      case InstallmentDeliverySigned(pdfBrokerageNotePath, subscriber) ⇒
        context.log.debug("BrokerageNoteWatcher -> Processing event InstallmentDeliverySigned({}, {}).", pdfBrokerageNotePath, subscriber)

        state.withDeliverySigned(pdfBrokerageNotePath, subscriber)

  @experimental def apply[F[_]](
    fileSystemWatcher: ActorRef[FileSystemWatcher.Command]
  )(using Timeout): Behavior[Command] =
    Behaviors.supervise {
      Behaviors.supervise {
        Behaviors.setup[Command] { context ⇒
          context.watch(fileSystemWatcher)
          
          val fileLifecycleResponseMapper: ActorRef[FileSystemWatcher.FileLifecycleEvent] =
            context.messageAdapter(
              fileLifecycleEvent ⇒ AdaptLifecycleResponse(fileLifecycleEvent)
            )
  
          Behaviors.withTimers { timerScheduler ⇒
            EventSourcedBehavior.withEnforcedReplies(
              persistenceId   = PersistenceId.ofUniqueId("BrokerageNoteWatcher"),
              emptyState      = State.empty[F],
              commandHandler  = (state, command)  ⇒ handleCommand(
                state, command, fileSystemWatcher, context, timerScheduler, fileLifecycleResponseMapper
              ),
              eventHandler    = (state, event)    ⇒ handleEvent[F](state, event, context)
            ).receiveSignal {
              case (_, signal: Terminated)        ⇒
                // FIXME Behaviors returned from Signal handling are ignored.
                //  In order to actually stopping if FileSystemWatcher stops,
                //  we need to use watchWith and handle that specific message
                //  by returning Behaviors.stopped
                context.log.debug("BrokerageNoteWatcher -> Signal received {}. It seems FileSystemWatcher has been terminated and I cannot proceed without it. Terminating too...", signal)
                Behaviors.stopped
              case (_, signal: PreRestart)        ⇒
                context.log.debug("BrokerageNoteWatcher -> Signal received {}. Something bad happened but, I'll be back in a moment...", signal)
                Behaviors.same
              case (_, signal: RecoveryCompleted) ⇒
                context.log.debug("BrokerageNoteWatcher -> Signal received {}. I'm back and I'll be going through the things pending in a moment...", signal)
  
                timerScheduler.startSingleTimer(
                  "brokerage-note-watcher-recovery-completed",
                  ResendPendingRequestsAndDeliveries,
                  500.millis
                )
  
                Behaviors.same
            }
          }
        }
      }.onFailure[ActorInitializationException] {
        SupervisorStrategy.stop
      }
    }.onFailure {
      SupervisorStrategy.restart
    }