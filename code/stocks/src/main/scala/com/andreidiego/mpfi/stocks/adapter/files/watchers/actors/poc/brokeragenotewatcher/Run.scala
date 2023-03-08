package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.brokeragenotewatcher

import java.nio.file.Path
import org.slf4j.LoggerFactory
import scala.annotation.experimental
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
import com.andreidiego.mpfi.stocks.adapter.files
import files.readers.pdf.poc.PDFBrokerageNotePath
import files.watchers.actors.poc.RequestId
import files.watchers.actors.poc.filewatcher.FileSystemWatcher
import BrokerageNoteWatcher.{PDFBrokerageNoteLifecycle, Request, RequestStatus, getClass}

@main @experimental def run(args: String*): Unit =
  val log = LoggerFactory.getLogger(getClass)

  log.debug("BrokerageNoteWatcher-Run -> Initializing the ActorSystem...")
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
  log.debug("BrokerageNoteWatcher-Run -> ActorSystem initialized.")

  log.debug("BrokerageNoteWatcher-Run -> Spawning FileSystemWatcher...")
  val eventualFileSystemWatcher: Future[ActorRef[FileSystemWatcher.Command]] = system.ask {
    UserGuardian.Spawn(FileSystemWatcher(), "FileSystemWatcher", Props.empty, _)
  }

  val brokerageNoteWatcher: Future[ActorRef[BrokerageNoteWatcher.Command]] =
    eventualFileSystemWatcher.flatMap { fileSystemWatcher ⇒
      log.debug("BrokerageNoteWatcher-Run -> Spawning BrokerageNoteWatcher...")
      system.ask {
        UserGuardian.Spawn(BrokerageNoteWatcher(fileSystemWatcher), "BrokerageNoteWatcher", Props.empty, _)
      }
    }

  log.debug("BrokerageNoteWatcher-Run -> Spawning PDFBrokerageNotePrinter...")
  val pDFBrokerageNotePrinter: Future[ActorRef[PDFBrokerageNoteLifecycle]] =
    system.ask {
      UserGuardian.Spawn(PDFBrokerageNotePrinter(), "PDFBrokerageNotePrinter", Props.empty, _)
    }

  log.debug("BrokerageNoteWatcher-Run -> Building commandProcessor...")
  val process = commandProcessor(brokerageNoteWatcher, pDFBrokerageNotePrinter)
  log.debug("BrokerageNoteWatcher-Run -> CommandProcessor ready.")

  while (true) {
    val command = StdIn.readLine("Type in the next command: ")
    println()
    process(command)
  }

  system.whenTerminated.onComplete {
    case Failure(exception) ⇒
      log.debug("BrokerageNoteWatcher-Run -> Failure detected when shutting down the actor system: {}", exception.getMessage)
      sys.exit(1)
    case Success(_) ⇒
      log.debug("BrokerageNoteWatcher-Run -> Actor system successfully shut down.")
      sys.exit()
  }

  addShutdownHook {
    log.debug("BrokerageNoteWatcher-Run -> Shutting down application...")
    log.debug("BrokerageNoteWatcher-Run -> Shutting down the actor system...")
    system.terminate()
    // Perform any cleanup operations here
  }

@experimental def commandProcessor(
  eventualBrokerageNoteWatcher    : Future[ActorRef[BrokerageNoteWatcher.Command]],
  eventualPDFBrokerageNotePrinter : Future[ActorRef[PDFBrokerageNoteLifecycle]]
)(using
  system  : ActorSystem[UserGuardian.Command],
  ec      : ExecutionContext,
  timeout : Timeout
): String ⇒ Unit = message ⇒
  log.debug("BrokerageNoteWatcher-Run -> Received message: {}", message)

  val (command, parameters) = message.span(_ != ' ')

  command match
    case "subscribe" ⇒
      val folder = Path.of(parameters.replaceAll("\"", "").strip())
      
      log.debug("BrokerageNoteWatcher-Run -> Subscribing for brokerage notes on {}.", folder)
      
      val result: Future[RequestId] = for
        brokerageNoteWatcher    <- eventualBrokerageNoteWatcher
        pdfBrokerageNotePrinter <- eventualPDFBrokerageNotePrinter
        answer                  <- brokerageNoteWatcher.askWithStatus[RequestId] {
          BrokerageNoteWatcher.SubscribeForPDFBrokerageNoteLifecycleEventsOn(folder, pdfBrokerageNotePrinter, None, _)
        }
      yield answer

      result.onComplete {
        case Success(requestId) ⇒
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher accepted the request and assigned it ID {}.", requestId)
        
        case Failure(ex)        ⇒
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "unsubscribe" =>
      val folder = Path.of(parameters.replaceAll("\"", "").strip())

      log.debug("BrokerageNoteWatcher-Run -> Unsubscribing from brokerage notes on {}.", folder)

      val result: Future[RequestId] = for
        brokerageNoteWatcher    <- eventualBrokerageNoteWatcher
        pdfBrokerageNotePrinter <- eventualPDFBrokerageNotePrinter
        answer                  <- brokerageNoteWatcher.askWithStatus[RequestId] {
          BrokerageNoteWatcher.UnsubscribeFromPDFBrokerageNoteLifecycleEventsOn(folder, pdfBrokerageNotePrinter, None, _)
        }
      yield answer

      result.onComplete {
        case Success(requestId) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher accepted the request and assigned it ID {}", requestId)

        case Failure(ex)        =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "requests" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher for an update on previously sent requests.")

      val result: Future[Map[Request, RequestStatus]] = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.ask {
          BrokerageNoteWatcher.GetStatuses
        }
      yield answer

      result.onComplete {
        case Success(requestStatuses) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher has reported the following requests:")

          requestStatuses.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)              =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "subscriptions" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher for the subscription map containing folders subscribed to and their subscribers.")

      val result: Future[Map[Path, Seq[ActorRef[PDFBrokerageNoteLifecycle]]]] = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.ask {
          BrokerageNoteWatcher.GetSubscriptions
        }
      yield answer

      result.onComplete {
        case Success(subscriptions) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher has reported the following subscriptions:")

          for (folder, subscribers) <- subscriptions
          do
            log.debug("- {}", folder)
            for subscriber <- subscribers
            do  log.debug("-- {}", subscriber)

        case Failure(ex)            =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "folders" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher for the list of folders currently being watched for brokerage notes.")

      val result: Future[Seq[Path]] = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.ask {
          BrokerageNoteWatcher.GetWatchedFolders
        }
      yield answer

      result.onComplete {
        case Success(watchers)  =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher has reported the following folders as currently being watched for brokerage notes:")

          watchers.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)        =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "brokeragenotes" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher for all the brokerage notes it knows about.")

      val result = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.ask[Seq[PDFBrokerageNotePath[cats.Id]]] {
          BrokerageNoteWatcher.GetBrokerageNotes
        }
      yield answer

      result.onComplete {
        case Success(brokerageNotes) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher has reported the following brokerage notes:")

          brokerageNotes.foreach {
            log.debug("- {}", _)
          }

        case Failure(ex)    =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "pendingdeliveries" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher for the deliveries that are still pending.")

      val result: Future[Iterable[(
        PDFBrokerageNotePath[_],
        ActorRef[PDFBrokerageNoteLifecycle],
        ActorRef[StatusReply[Done]] => PDFBrokerageNoteLifecycle
      )]] =
        for
          brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
          answer                <- brokerageNoteWatcher.ask {
            BrokerageNoteWatcher.GetPendingDeliveries[cats.Id]
          }
        yield answer

      result.onComplete {
        case Success(deliveries)  =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher has reported the following pending deliveries:")

          for (folder, subscriber, _) <- deliveries
          do  log.debug("- {} -> {}", folder, subscriber)

        case Failure(ex)          =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "restart" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher to restart.")

      val result: Future[Done] = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.askWithStatus {
          BrokerageNoteWatcher.Restart
        }
      yield answer

      result.onComplete {
        case Success(ack) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher got the message and should be restarting soon.")

        case Failure(ex)  =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case "stop" =>
      log.debug("BrokerageNoteWatcher-Run -> Asking BrokerageNoteWatcher to stop.")

      val result: Future[Done] = for
        brokerageNoteWatcher  <- eventualBrokerageNoteWatcher
        answer                <- brokerageNoteWatcher.askWithStatus {
          BrokerageNoteWatcher.Stop
        }
      yield answer

      result.onComplete {
        case Success(ack) =>
          log.debug("BrokerageNoteWatcher-Run -> BrokerageNoteWatcher got the message and should be stopping soon.")

        case Failure(ex)  =>
          log.debug("BrokerageNoteWatcher-Run -> Failure: {}", ex.getMessage)
      }

    case c ⇒ log.debug("BrokerageNoteWatcher-Run -> Invalid command: {}.", c)

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
          context.log.debug("Spawning a new behavior {}...", behavior)

          val ref = context.spawn(behavior, name, props)
          context.watchWith(ref, ActOnTerminationOf(ref))
          replyTo ! ref
          Behaviors.same

        // FIXME I have to figure out why the actor system is
        //  not being stopped when its user guardian is stopped
        case ActOnTerminationOf(ref) =>
          context.log.debug("{} has terminated and I cannot do anything without it so, I'm stopping too...", ref)
          Behaviors.stopped
      }
    }

object PDFBrokerageNotePrinter:
  
  @experimental def apply(): Behavior[PDFBrokerageNoteLifecycle] =
    Behaviors.receive { (context, message) ⇒ message match
      
      case BrokerageNoteWatcher.PDFBrokerageNoteCreated(pdfBrokerageNote, ackTo) ⇒
        context.log.debug("PDFBrokerageNotePrinter -> Just heard about the creation of {}.", pdfBrokerageNote)

        ackTo ! StatusReply.ack()
        Behaviors.same
        
      case BrokerageNoteWatcher.PDFBrokerageNoteRemoved(pdfBrokerageNote, ackTo) ⇒
        context.log.debug("PDFBrokerageNotePrinter -> Just heard about the removal of {}.", pdfBrokerageNote)

        ackTo ! StatusReply.ack()
        Behaviors.same
    }