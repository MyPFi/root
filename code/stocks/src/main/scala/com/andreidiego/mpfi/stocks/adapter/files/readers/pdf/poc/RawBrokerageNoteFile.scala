package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import scala.annotation.experimental
import utils.NonEmptyString

class RawBrokerageNoteFile private(val documentText: NonEmptyString)

@experimental object RawBrokerageNoteFile:
  import java.io.File
  import org.apache.pdfbox.Loader.loadPDF
  import org.apache.pdfbox.text.PDFTextStripper
  import cats.data.Validated
  import cats.syntax.validated.catsSyntaxValidatedId

  enum RawBrokerageNoteFileError:
    @experimental case ParsingError(reason: Throwable)
    @experimental case EmptyDocument

  import RawBrokerageNoteFileError.*
  
  type Error = RawBrokerageNoteFileError
  type ErrorOr[A] = Validated[Error, A]

  def from[F[_]](pdfBrokerageNotePath: PDFBrokerageNotePath[F]): ErrorOr[RawBrokerageNoteFile] = {
    /**
     * What can go wrong?
     *  getText     -> IOException              - if the doc state is invalid or it is encrypted
     *  loadPDF     -> InvalidPasswordException - If the file required a non-empty password.
     *                 IOException              - in case of a file reading or parsing error
     *
     *  new File()  -> NullPointerException     – If the pathname argument is null**
     *                                            **Guaranteed no to happen by PDFBrokerageNotePath[F]
     * */
    val stripper = PDFTextStripper()
    stripper.setSortByPosition(true)
    
    try {
      val documentText = NonEmptyString {
        stripper.getText(loadPDF(new File(pdfBrokerageNotePath.path))).strip()
      }
      
      documentText
        .map(RawBrokerageNoteFile(_).valid)
        .getOrElse(EmptyDocument.invalid)
      
    } catch {
      case e: Throwable ⇒ ParsingError(e).invalid
    }
  }