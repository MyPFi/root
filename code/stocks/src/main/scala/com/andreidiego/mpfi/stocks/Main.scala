package com.andreidiego.mpfi.stocks

import com.andreidiego.mpfi.stocks.{LoopInterpreter, SequentialInterpreter}
import org.apache.pdfbox.Loader.loadPDF
import org.apache.pdfbox.text.PDFTextStripper
import org.rogach.scallop.*
import os.{Path, up}

import java.io.File
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

@main def mpfi(args: String*): Unit =
  val conf = Conf(args)

  if conf.document.isDefined then
    println(PDFTextStripper().getText(loadPDF(conf.document())))

  else {
    val rootFolder = os.Path("F:/OneDrive/Documentos/Financeiros/Investimentos/Bolsa/Notas de Corretagem")

    if os.exists(rootFolder / "result.txt") then
      os.remove(rootFolder / "result.txt")

    processTransactionConfirmationsFrom(rootFolder)
  }

def processTransactionConfirmationsFrom(folder: Path): Unit =
  val templateNamePattern = raw"nota_corretagem-modal_mais-v(\d+)".r.unanchored
  val datePattern = raw"(\d{2})-(\d{2})-(\d{4})".r.unanchored

  val yearPattern = raw"(\d{4})".r.unanchored
  val templateNames = os.list(folder / "Templates")
    .map(_.toString)
    .sortWith((_, _) match
      case (templateNamePattern(version1), templateNamePattern(version2)) =>
        version1.toInt > version2.toInt
    )

//  interpret("F:/OneDrive/Documentos/Financeiros/Investimentos/Bolsa/Notas de Corretagem/2021/compraAZUL4-EMBR3 - 85060 - 22-10-2021.pdf")(templateNames)
  os.list(folder)
    .filter(os.isDir(_))
    .filter(osPath => yearPattern.matches(osPath.toString))
    .flatMap(os.list)
    .map(_.toString)
    .filter(_ endsWith ".pdf")
    .sortWith((_, _) match
      case (datePattern(day1, month1, year1), datePattern(day2, month2, year2)) =>
        (year1 + month1 + day1).toInt > (year2 + month2 + day2).toInt
    )
    .foreach(fileName => interpret(fileName)(templateNames))
//    .map(fileName => Future(interpret(fileName)(templateNames)))

@tailrec
def interpret(fileName: String)(templateNames: Seq[String]): Unit =
  val templateName = templateNames.head
//  val templateName = "F:/OneDrive/Documentos/Financeiros/Investimentos/Bolsa/Notas de Corretagem/Templates/nota_corretagem-modal_mais-v13.txt"
  println(s"\nApplying $templateName to $fileName.")

  Try {
    val documentText = PDFTextStripper().getText(loadPDF(new File(fileName)))
    val templateText = os.read(os.Path(templateName), "ISO-8859-1")
    val repetitionPattern = raw"R-L(\d+)...(\d+)".r.unanchored

    templateText match
      case repetitionPattern(start, finish) =>
        val repetitionInstruction = s"R-L$start...$finish"
        val firstOccurrence = templateText.indexOf(repetitionInstruction)
        val nextOccurrence = templateText.indexOf(repetitionInstruction, firstOccurrence + 1)
        val repetitionBlock = templateText.substring(firstOccurrence, nextOccurrence + repetitionInstruction.length)

        LoopInterpreter(repetitionBlock.split("\n").toList)
          .interpret(documentText)

        SequentialInterpreter(
          templateText.substring(0, firstOccurrence).concat(templateText.substring(nextOccurrence + repetitionInstruction.length + 2)).split("\n").toList
        ).interpret(documentText)

      case _ =>
        SequentialInterpreter(templateText.split("\n").toList)
          .interpret(documentText)

  } match {
    case Failure(e) if templateNames.tail.nonEmpty => interpret(fileName)(templateNames.tail)
    case Failure(e) if templateNames.tail.isEmpty => throw e
    case _ =>
      val fileNameParts = fileName.split('\\')
      val templateNameParts = templateName.split('\\')

      os.write.append(
        os.Path(fileName) / up / up / "result.txt",
        s"${fileNameParts(fileNameParts.length - 1)} -> ${templateNameParts(templateNameParts.length - 1)}\n"
      )
  }

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) :
  //  val template: ScallopOption[File] = opt[File]()
  val document: ScallopOption[File] = opt[File]()

  //  codependent(template, document)
  //  validateFileExists(template)
  validateFileExists(document)
  verify()