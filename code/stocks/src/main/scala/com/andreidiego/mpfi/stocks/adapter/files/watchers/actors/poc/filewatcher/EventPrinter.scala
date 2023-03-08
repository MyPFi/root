package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.filewatcher

import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.ExecutionContext
import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.stream.scaladsl.{Sink, Source}
import FileSystemWatcher.getClass

private val log: Logger = LoggerFactory.getLogger(getClass)

@main def print(args: String*): Unit =
  log.debug("EventPrinter -> Initializing the ActorSystem...")
  given system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "Event-Printer")
  given ExecutionContext = system.executionContext
  log.debug("EventPrinter -> ActorSystem initialized.")

  log.debug("EventPrinter -> Trying to obtain a read journal...")
  val readJournal: EventsByPersistenceIdQuery = PersistenceQuery(system)
    .readJournalFor[EventsByPersistenceIdQuery]("jdbc-read-journal")
  log.debug("EventPrinter -> Successfully obtained the read journal {}.", readJournal)

  val persistenceId   = "FileSystemWatcher"
  val fromSequenceNr  = 0L
  val toSequenceNr    = Long.MaxValue

  log.debug("EventPrinter -> Creating a source of events for persistenceId {}...", persistenceId)
  val events: Source[EventEnvelope, NotUsed] =
    readJournal.eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr)
  log.debug("EventPrinter -> Source of events for persistenceId {} has been created.", persistenceId)

  log.debug("EventPrinter -> Events for persistenceId {} should start coming anytime...", persistenceId)
  events
    .runWith {
      Sink.foreach {
        log.debug("EventPrinter -> New event identified for persistenceId {}: {}.", persistenceId, _)
      }
    }
    .onComplete { _ =>
      log.debug("EventPrinter -> Source of events for persistenceId {} has been exhausted. Terminating the system...", persistenceId)
      system.terminate()
    }