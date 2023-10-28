package com.andreidiego.mpfi.stocks.brokeragenotes.adapter.files.readers.pdf.poc

import com.andreidiego.extractionguide
import extractionguide.FieldName

sealed trait BrokerageNotePart
case class BrokerageNoteHeaderField(fieldName: FieldName, fieldValue: String)                 extends BrokerageNotePart
case class BrokerageNoteHeader(brokerageNoteHeaderFields: Iterable[BrokerageNoteHeaderField]) extends BrokerageNotePart
case class BrokerageNoteItemField(fieldName: FieldName, fieldValue: String)                   extends BrokerageNotePart
case class BrokerageNoteOperation(brokerageNoteItemFields: Iterable[BrokerageNoteItemField])  extends BrokerageNotePart
case class BrokerageNoteOperations(brokerageNoteOperations: Iterable[BrokerageNoteOperation]) extends BrokerageNotePart
case class TokenizedBrokerageNote private (
  brokerageNoteHeader: BrokerageNoteHeader,
  brokerageNoteOperations: BrokerageNoteOperations
)                                                                                             extends BrokerageNotePart

object TokenizedBrokerageNote:
  import org.slf4j.LoggerFactory
  import scala.util.Try
  import extractionguide.{ExtractionGuide, FixedTokenCoordinate, RegularInstruction, RelativeTokenCoordinate, RepetitionBlock}

  enum Exception(cause: Throwable) extends Throwable:
    initCause(cause)
    case ParsingError(cause: Throwable) extends Exception(cause)
//    case RequiredValueMissing(fieldName: FieldName)

  import Exception.*

  private val log = LoggerFactory.getLogger(getClass)

  // TODO Introduce a dependency to a to-be-created effectful
  //  Logging module and use it to replace all the debug statements
  // FIXME accumulate errors instead of failing fast with an exception
  def from(
    rawBrokerageNoteFile: RawBrokerageNoteFile,
    extractionGuide: ExtractionGuide
  ): Try[TokenizedBrokerageNote] = Try {
    /**
     * What can go wrong?
     *  documentLines(lineNumber)     ->  ArrayIndexOutOfBoundsException
     *  substring(columnNumber)       ->  IndexOutOfBoundsException – if beginIndex is negative or larger than the length of this String object
     *  fieldValue                    ->  Empty
     *  documentLines(lineNumber - 1) ->  ArrayIndexOutOfBoundsException
     **/
    try {
      val documentLines = rawBrokerageNoteFile.documentText.split("\n")

      val brokerageNoteParts: Iterable[BrokerageNotePart] =
        extractionGuide.instructions.map {
          case regularInstruction: RegularInstruction =>
            val matrixCoordinate = regularInstruction.matrixCoordinate
            val lineNumber: Int = matrixCoordinate.lineNumber - 1
            val columnNumber: Int = matrixCoordinate.columnNumber - 1

            log.debug(s"TokenizedBrokerageNote: regularInstruction.fieldName: ${regularInstruction.fieldName}")
            log.debug(s"TokenizedBrokerageNote: regularInstruction.matrixCoordinate: ${matrixCoordinate.lineNumber}, ${matrixCoordinate.columnNumber}")
            log.debug(s"TokenizedBrokerageNote: regularInstruction.fieldType: ${regularInstruction.fieldType}")
            log.debug(s"TokenizedBrokerageNote: regularInstruction.fieldType.regex: ${regularInstruction.fieldType.regex}")

            val fieldValue = regularInstruction.fieldType.regex.findFirstIn(
              documentLines(lineNumber).substring(columnNumber).strip().replaceFirst("\u00a0", "")
            ).getOrElse("") //(throw RequiredValueMissing(regularInstruction.fieldName))

            log.debug(s"TokenizedBrokerageNote: regularInstruction.fieldValue: $fieldValue")

            BrokerageNoteHeaderField(regularInstruction.fieldName, fieldValue)

          case repetitionBlock: RepetitionBlock =>
            val lineRange = repetitionBlock.lineRange
            log.debug(s"TokenizedBrokerageNote: repetitionBlock.lineRange: $lineRange")

            val operations: Seq[BrokerageNoteOperation] = {
              if repetitionBlock.jumpLine then
                log.debug(s"TokenizedBrokerageNote: repetitionBlock.jumpLine: ${repetitionBlock.jumpLine}")
                lineRange.start to lineRange.end by 2

              else
                lineRange.start to lineRange.end

            }.map { lineNumber ⇒
              log.debug(s"TokenizedBrokerageNote: repetitionBlock.lineNumber: $lineNumber")

              val operation: Iterable[BrokerageNoteItemField] =
                repetitionBlock.repeatedItems.map { repeatedItem ⇒

                  log.debug(s"TokenizedBrokerageNote: repetitionBlock.repeatedItem.fieldName: ${repeatedItem.fieldName}")
                  log.debug(s"TokenizedBrokerageNote: repetitionBlock.repeatedItem.fieldType: ${repeatedItem.fieldType}")
                  log.debug(s"TokenizedBrokerageNote: repetitionBlock.repeatedItem.tokenCoordinate: ${repeatedItem.tokenCoordinate}")

                  val tokens: Array[String] = documentLines(lineNumber - 1)
                    .strip()
                    .replaceFirst("\u00a0", "")
                    .split(" ")
                    .filter(_.nonEmpty)

                  log.debug(s"TokenizedBrokerageNote: repetitionBlock.repeatedItem.tokens: $tokens")

                  def tokenNumber(numberOfTokens: Int): Int = repeatedItem.tokenCoordinate match
                    case FixedTokenCoordinate(tokenIndex) ⇒
                      tokenIndex - 1

                    case RelativeTokenCoordinate(_, _, adjustingFactor) ⇒
                      numberOfTokens - adjustingFactor - 1

                  val fieldValue: String = repeatedItem.fieldType.regex.findFirstIn(
                    if tokens.isEmpty then ""
                    else tokens(tokenNumber(tokens.length))
                      .strip()
                      .replaceFirst("\u00a0", "")
                  ).getOrElse("") //(throw RequiredValueMissing(repeatedItem.fieldName))

                  log.debug(s"TokenizedBrokerageNote: repetitionBlock.repeatedItem.fieldValue: $fieldValue")

                  BrokerageNoteItemField(repeatedItem.fieldName, fieldValue)
                }

              BrokerageNoteOperation(operation)
            }

            BrokerageNoteOperations(operations)
        }

      val (
        header    : Iterable[BrokerageNoteHeaderField],
        operations: Iterable[BrokerageNoteOperations]
      ) = brokerageNoteParts.partition {
        case BrokerageNoteHeaderField(_, _) ⇒ true
        case _ ⇒ false
      }: @unchecked

      TokenizedBrokerageNote(BrokerageNoteHeader(header), operations.head)

    } catch {
//      case e: RequiredValueMissing ⇒ throw e
      case e ⇒ throw ParsingError(e)
    }
  }