package com.andreidiego.mpfi.stocks.brokeragenotes.adapter.files.readers.pdf.poc

import java.nio.file.Path
import org.slf4j.LoggerFactory
import scala.annotation.experimental
import scala.util.Try
import com.andreidiego.filesystem.FileSystemPath

private val log = LoggerFactory.getLogger("Brokerage-Notes-PDF-Reader")

@experimental @main def run(args: String*): Unit =
  val rootFolder = s"${BrokerageNotesPDFReader.getClass.getResource(".").getPath.drop(1)}"

  log.debug("About to read brokerage note data from PDF files on: {}", rootFolder)

  program[cats.Id] {
    os.Path(rootFolder).toNIO
  }

@experimental private def program[F[_]](
  rootFolder: Path
): FileSystemPath.InteractsWithTheFileSystemAndReturns[Try[Unit]][F] =
  import cats.data.NonEmptyChain
  import cats.data.NonEmptySeq
  import cats.syntax.apply.*
  import cats.syntax.functor.*
  import com.andreidiego.extractionguide.ExtractionGuideSpecsPath

  (
    PDFBrokerageNotesPath.from[F](rootFolder.toString),
    ExtractionGuideSpecsPath.from[F](rootFolder.resolve("Templates").toString)

  ).mapN { (tentativePDFBrokerageNotesPath, tentativeExtractionGuideSpecsPath) ⇒

    (tentativePDFBrokerageNotesPath, tentativeExtractionGuideSpecsPath)
      .mapN { (pdfBrokerageNotesPath, extractionGuideSpecsPath) ⇒

        BrokerageNotesPDFReader
          .from[F](NonEmptySeq.one(
            (pdfBrokerageNotesPath, extractionGuideSpecsPath)
          ))
          .map { errorsOrBrokerageNotesPDFReader ⇒
            errorsOrBrokerageNotesPDFReader.fold (
              (errors: NonEmptyChain[BrokerageNotesPDFReader.Error])  ⇒
                errors.toNonEmptyList.toList.foreach(e ⇒ log.debug("errors: {}", e)),
              (b: BrokerageNotesPDFReader[F]) ⇒
                b.brokerageNotes.foreach(a ⇒ log.debug("brokerageNotes: {}", a))
            )
          }
      }
  }