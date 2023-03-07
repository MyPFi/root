package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import scala.annotation.experimental

class BrokerageNotesPDFReader[F[_]](val brokerageNotes: Set[TokenizedBrokerageNote])

@experimental object BrokerageNotesPDFReader:
  import org.slf4j.LoggerFactory
  import scala.annotation.tailrec
  import scala.util.{Failure, Success}
  import cats.implicits.*
  import cats.data.{NonEmptySeq, ValidatedNec}
  import cats.data.Validated.{Valid, Invalid}
  import cats.syntax.apply.catsSyntaxTuple2Semigroupal
  import cats.syntax.validated.catsSyntaxValidatedId
  import cats.syntax.traverse.toTraverseOps
  import com.andreidiego.mpfi.stocks.adapter.files
  import files.FileSystemPath.InteractsWithTheFileSystemAndReturns
  import files.extractionguide.poc.{ExtractionGuide, ExtractionGuideSpecPath}
  import files.extractionguide.poc.ExtractionGuide.ExtractionGuideError.{ParsingError, FileCannotBeOpened}

  enum Exception(message: String) extends Throwable:
    @experimental case InvalidExtractionGuide(message: String = "", reason: Option[Throwable])
      extends Exception(
        if message.nonEmpty then
          s"""Message: $message. ${reason.map(m ⇒ s"Reason: ${m.getMessage}").getOrElse("")}"""

        else s"""${reason.map(m ⇒ s"Reason: ${m.getMessage}").getOrElse("")}"""
      )

  enum BrokerageNotesReaderError(val message: String):
    @experimental case InvalidRawBrokerageNoteFile(reason: Throwable)               extends BrokerageNotesReaderError(reason.toString)
    @experimental case InvalidTokenizedBrokerageNote(override val message: String)  extends BrokerageNotesReaderError(message)
    @experimental case NoValidTokenizedBrokerageNoteFound                           extends BrokerageNotesReaderError("")

  import Exception.*
  import BrokerageNotesReaderError.*

  type Error = BrokerageNotesReaderError | RawBrokerageNoteFile.Error | ExtractionGuide.Error
  type ErrorsOr[A] = ValidatedNec[Error, A]

  private val log = LoggerFactory.getLogger(getClass)

  /**
   * <pre>
   * Initial specification (Probably already different from how it's been implemented).
   *
   * BrokerageNotesPDFReader.from(NonEmptySeq((PDFBrokerageNotesPath, ExtractionGuideSpecsPath), Seq())) =
   *   For each pair
   *     Get all ExtractionGuideSpecPaths ordered by DocumentVersion ascending
   *     Get all PDFBrokerageNotePaths ordered by TradingDate ascending
   *
   *     For each ExtractionGuideSpecPath
   *       If there's no PDFBrokerageNotePath to be processed
   *         Log a warning stating that the ExtractionGuideSpecPath is not being used
   *       Otherwise
   *         Parse its contents into an ExtractionGuide (Abort on failure)
   *           // ExtractionGuide.from(ExtractionGuideSpecPath)
   *           For each PDFBrokerageNotePath
   *             Parse its contents into a RawBrokerageNoteFile (Abort on failure)
   *               // RawBrokerageNoteFile.from(PDFBrokerageNotePath)
   *             Parse the RawBrokerageNoteFile into an InMemoryBrokerageNote using the current ExtractionGuide
   *               // InMemoryBrokerageNote.from(RawBrokerageNoteFile, ExtractionGuide)
   *               If parsing fails
   *                 Log the failed parsing attempt as a warning
   *                 As long as there are PDFBrokerageNotes left to parse, move to the next ExtractionGuideSpec and recurse
   *               Accumulate the resulting InMemoryBrokerageNote
   *     If the ExtractionGuideSpecs are exhausted when there are still PDFBrokerageNotes to be parsed
   *       Log an error containing
   *         The number of files already parsed
   *         The number of files still unparsed
   *         The name of the first unparsed PDFBrokerageNote
   *       Abort
   *   Return a new BrokerageNotesPDFReader containing the accumulated set of InMemoryBrokerageNotes
   * </pre>
   * */
  def from[F[_]](pathsToPDFBrokerageNotesAndTheirExtractionGuides: NonEmptySeq[
    (PDFBrokerageNotesPath[F], ExtractionGuideSpecsPath[F])
  ]): InteractsWithTheFileSystemAndReturns[ErrorsOr[BrokerageNotesPDFReader[F]]][F] =

    pathsToPDFBrokerageNotesAndTheirExtractionGuides.map { (pdfBrokerageNotesPath, extractionGuideSpecsPath) ⇒
      (
        extractionGuideSpecsPath.children(ExtractionGuideSpecsPath.Sorters.documentVersionSorter),
        pdfBrokerageNotesPath.children(PDFBrokerageNotesPath.Sorters.tradingDateSorter)

      ).mapN { (orderedExtractionGuideSpecPaths, orderedPDFBrokerageNotePaths) ⇒

        tokenizedBrokerageNotesFrom(
          orderedExtractionGuideSpecPaths,
          orderedPDFBrokerageNotePaths
        )
      }

    }.sequence.map { nesErrorsOrTokenizedBrokerageNotes ⇒
      nesErrorsOrTokenizedBrokerageNotes
        .reduce(_ combine _)
        .map { tokenizedBrokerageNotes ⇒
          log.debug("BrokerageNotesPDFReader: tokenizedBrokerageNotes: {}", tokenizedBrokerageNotes)
          BrokerageNotesPDFReader[F](tokenizedBrokerageNotes.toSet)
        }
    }

  @tailrec
  private def tokenizedBrokerageNotesFrom[F[_]](
    extractionGuideSpecPaths: LazyList[ExtractionGuideSpecPath[F]],
    pdfBrokerageNotePaths   : LazyList[PDFBrokerageNotePath[F]],
    acc                     : Seq[TokenizedBrokerageNote] = Seq.empty
  ): ErrorsOr[Seq[TokenizedBrokerageNote]] =

    if pdfBrokerageNotePaths.isEmpty then
      if extractionGuideSpecPaths.nonEmpty then
        extractionGuideSpecPaths.foreach { egsp =>
          log.debug("INFO: {} is not been used.", egsp.path)
        }

      if acc.isEmpty then
        NoValidTokenizedBrokerageNoteFound.invalidNec
      else acc.validNec

    else if extractionGuideSpecPaths.isEmpty then
      val pdfBrokerageNotePath = pdfBrokerageNotePaths.head

      RawBrokerageNoteFile.from[F](pdfBrokerageNotePath) match
        case Invalid(exception) ⇒
          log.debug("BrokerageNotesPDFReader: extractionGuideSpecPaths.isEmpty.rawBrokerageNoteFile.exception: {}", exception)

        case Valid(rawBrokerageNoteFile) ⇒
          log.debug("BrokerageNotesPDFReader: extractionGuideSpecPaths.isEmpty.rawBrokerageNoteFile.rawBrokerageNoteFile.")
          log.debug("WARNING: No extraction guide was found that could parse {}. It is possible that the brokerage firm changed its brokerage note's template and you still don't have an extraction guide for the new one. Here is the diagram for the last brokerage note that could not be parsed in case you want to create a new extraction guide for it:", pdfBrokerageNotePath.path)

          val documentLines = rawBrokerageNoteFile.documentText.split("\n")
          log.debug("BrokerageNotesPDFReader: tokenizedBrokerageNotesFrom.documentLines.length: {}", documentLines.length)

          val sizeOfLongestLine = documentLines.foldLeft(0) { (acc, currentLine) ⇒ {
            val lineLength = currentLine.length
            if lineLength > acc then lineLength else acc
          }}

          var lineNumber = 1

          val c = 'C'
          var columnHeader = f"$c%4s"
          (1 until sizeOfLongestLine).foreach { index ⇒
            columnHeader = columnHeader + f"$index%4s"
          }
          log.debug(columnHeader)

          val l = 'L'
          log.debug(f"$l%2s")

          documentLines.foreach { line =>
            var toBePrinted = f"$lineNumber%3s "

            line.toCharArray.foreach { c ⇒
              toBePrinted = toBePrinted + f"$c%4s".replaceAll(" ", ".")
            }

            log.debug(toBePrinted)
            lineNumber = lineNumber + 1
          }

      if acc.isEmpty then
        NoValidTokenizedBrokerageNoteFound.invalidNec
      else acc.validNec

    else
      val extractionGuideSpecPath           = extractionGuideSpecPaths.head
      val extractionGuide: ExtractionGuide  = ExtractionGuide.from[F](extractionGuideSpecPath) match
        case Invalid(exception) ⇒
          log.debug("BrokerageNotesPDFReader: extractionGuide.exception: {}", exception)

          exception match
            case FileCannotBeOpened(cause)  ⇒ throw InvalidExtractionGuide(reason = Some(cause))
            case p: ParsingError            ⇒ throw InvalidExtractionGuide(p.message, None)

        case Valid(extractionGuide) ⇒
          log.debug("BrokerageNotesPDFReader: extractionGuide.extractionGuide: {}", extractionGuide.instructions)
          extractionGuide

      val pdfBrokerageNotePath = pdfBrokerageNotePaths.head
      RawBrokerageNoteFile.from[F](pdfBrokerageNotePath) match
        case Invalid(exception) ⇒
          log.debug("BrokerageNotesPDFReader: RawBrokerageNoteFile.exception: {}", exception)
          tokenizedBrokerageNotesFrom(extractionGuideSpecPaths, pdfBrokerageNotePaths.tail, acc)

        case Valid(rawBrokerageNoteFile) ⇒
          log.debug("BrokerageNotesPDFReader: RawBrokerageNoteFile.rawBrokerageNoteFile") //: ${rawBrokerageNoteFile.documentText}")

          TokenizedBrokerageNote.from(rawBrokerageNoteFile, extractionGuide) match
            case Failure(exception) ⇒
              log.debug("BrokerageNotesPDFReader: TokenizedBrokerageNote.exception: {}", exception.getCause)

              val message =
                s"""WARNING: ${pdfBrokerageNotePath.path} could not be parsed using ${extractionGuideSpecPath.path}.
                   |Will try with the next 'ExtractionGuide' available, if any.
                   |Root cause: $exception
                   |""".stripMargin
              log.debug("message: {}", message)

              tokenizedBrokerageNotesFrom(
                extractionGuideSpecPaths.tail,
                pdfBrokerageNotePaths,
                acc
              )

            case Success(tokenizedBrokerageNote) ⇒
              log.debug("BrokerageNotesPDFReader: TokenizedBrokerageNote.tokenizedBrokerageNote: {}", tokenizedBrokerageNote)

              tokenizedBrokerageNotesFrom(
                extractionGuideSpecPaths,
                pdfBrokerageNotePaths.tail,
                tokenizedBrokerageNote +: acc
              )