package com.andreidiego.mpfi.stocks.adapter.files.extractionguide.poc

class ExtractionGuide(val instructions: Iterable[Instruction])

object ExtractionGuide:
  import java.io.FileReader
  import org.slf4j.LoggerFactory
  import scala.annotation.experimental
  import cats.data.Validated
  import cats.syntax.validated.*
  import ExtractionGuideSpecCompiler.*

  enum ExtractionGuideError(val message: String):
    case FileCannotBeOpened(cause: Throwable) extends ExtractionGuideError(cause.getMessage)
    case ParsingError(path: String, encoding: String, reason: String) extends ExtractionGuideError(
      s"Failure while parsing $path!. Reason: $reason. Make sure that $path ExtractionGuideSpec's encoding ($encoding) is UTF-8."
    )

  import ExtractionGuideError.*
  
  type Error = ExtractionGuideError
  type ErrorOr[A] = Validated[Error, A]

  private val log = LoggerFactory.getLogger(getClass)

  // TODO Introduce a dependency to a to-be-created effectful Logging module 
  @experimental def from[F[_]](extractionGuideSpecPath: ExtractionGuideSpecPath[F]): ErrorOr[ExtractionGuide] =
    /**
     * What can go wrong?
     *
     *  new FileReader()  -> FileNotFoundException – if the named file
     *                        - does not exist,**
     *                        - is a directory rather than a regular file,**
     *                      --> for some other reason, cannot be opened for reading. <--
     *                        **Guaranteed not to happen by ExtractionGuideSpecPath[F]
     *  parseAll()        -> Syntax errors
     *                       File not encoded in UTF-8
     * */
    try
      val extractionGuideSpec = new FileReader(extractionGuideSpecPath.path)

      parseAll(extractionGuideCompiler, extractionGuideSpec) match
        case Success(result, _) ⇒
          log.debug("ExtractionGuide -> Successfully parsed {}. Parsing result: {}", extractionGuideSpecPath.path, result.instructions)
          ExtractionGuide(result.instructions).valid
        case NoSuccess(msg, _) ⇒
          log.debug("ExtractionGuide -> Failure while parsing {}!. Msg: {}", extractionGuideSpecPath.path, msg)
          ParsingError(extractionGuideSpecPath.path, extractionGuideSpec.getEncoding, msg).invalid
    catch
      case e: Throwable ⇒ FileCannotBeOpened(e).invalid