package com.andreidiego.mpfi.stocks.adapter.files.extractionguide.poc

import org.slf4j.LoggerFactory
import scala.annotation.experimental

private val log = LoggerFactory.getLogger("Extraction-Guide")

@experimental @main def run(args: String*): Unit =
  val extractionGuideSpecPath =
    s"${ExtractionGuideSpecCompiler.getClass().getResource(".").getPath.drop(1)}nota_corretagem-modal_mais-v1.txt"

  log.debug("About to compile {} into an ExtractionGuide.", extractionGuideSpecPath)

  fromCompiler(extractionGuideSpecPath)
  fromSmartConstructor(extractionGuideSpecPath)

@experimental def fromCompiler(extractionGuideSpecPath: String) =
  import java.io.FileReader
  import ExtractionGuideSpecCompiler.{extractionGuideCompiler, parseAll, Success, NoSuccess}

  // Files must be encoded in UTF-8
  val extractionGuideSpec = new FileReader(extractionGuideSpecPath)

  log.debug("---------- From compiler ----------")
  log.debug("FileReader encoding : {}", extractionGuideSpec.getEncoding)
  log.debug ("{}", parseAll(extractionGuideCompiler, extractionGuideSpec) match
    case Success(result, next) ⇒
      s"""result:
         |\t${result.instructions.mkString("\n\t")}.
         |
         |atEnd? ${next.atEnd}.""".stripMargin
    case NoSuccess(msg) ⇒ msg
  )

@experimental def fromSmartConstructor(extractionGuideSpecPath: String) =
  import unsafeExceptions.canThrowAny

  log.debug("---------- From smart constructor ----------")
  ExtractionGuide
    .from(ExtractionGuideSpecPath.from(extractionGuideSpecPath))
    .fold(
      (error: ExtractionGuide.Error) =>
        log.debug ("Unable to get an extraction guide. Failure: {}", error.message),
      (extractionGuide: ExtractionGuide) =>
        log.debug ("Extraction Guide: \n\t{}", extractionGuide.instructions.mkString("\n\t"))
    )