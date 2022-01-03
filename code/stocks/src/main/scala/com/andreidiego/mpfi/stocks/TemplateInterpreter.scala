package com.andreidiego.mpfi.stocks

import scala.util.matching.Regex

sealed trait TemplateInterpreter:
  def interpret(documentText: String): Unit

  def fieldNameFrom(lineColumns: Array[String]): String = lineColumns(0).strip()

  def regexFor(dataType: DataType): Regex =
    dataType match {
      case DataType.Number => raw"([-\u00AD]?)(\d+)".r
      case DataType.Character => raw"(^[a-zA-Z0-9])".r
      case DataType.String => raw"(^[a-zA-Z0-9]+)".r
      case DataType.Date => raw"(\d{2})/(\d{2})/(\d{4})".r
      case DataType.Currency => raw"^([-\u00AD]?)(\p{Sc}?)(([1-9]\d{0,2}(\.\d{3})*)|(([1-9]\d*)?\d))(,\d\d)?".r
    }

  def fieldTypeFrom(lineColumns: Array[String]): String = lineColumns(2).strip()

case class SequentialInterpreter(instructions: List[String]) extends TemplateInterpreter :
  override def interpret(documentText: String): Unit =
    instructions.foreach(instruction => println(correspondingValueFrom(documentText)(instruction)))

  def correspondingValueFrom(documentText: String)(lineInTemplate: String): String =
    val lineColumns = lineInTemplate.split("->")
    s"${fieldNameFrom(lineColumns)} -> ${fieldValueFrom(documentText)(lineColumns)}"

  def fieldValueFrom(documentText: String)(lineColumns: Array[String]): String =
    val documentLines = documentText.split("\n") //.filter(_ != "")
    val regex = regexFor(DataType(fieldTypeFrom(lineColumns)))

    regex.findFirstIn(
      documentLines(lineNumberFrom(lineColumns(1))).substring(columnNumberFrom(lineColumns(1))).strip().replaceFirst("\u00a0", "")
    ).getOrElse("")

  def lineNumberFrom(coordinate: String): Int = {
    withCoordinate(coordinate) {
      case (line, column) => line.toInt - 1
    }
  }

  def columnNumberFrom(coordinate: String): Int = {
    withCoordinate(coordinate) {
      case (line, column) => column.toInt - 1
    }
  }

  def withCoordinate(coordinate: String)(actUpon: (String, String) => Int): Int =
    val coordinatePattern = raw"L(\d+)C(\d+)".r

    coordinate.strip() match {
      case coordinatePattern(line, column) => actUpon(line, column)
    }

case class LoopInterpreter(instructions: List[String]) extends TemplateInterpreter :

  override def interpret(documentText: String): Unit =
    val documentLines = documentText.split("\n") //.filter(_ != "")
    val repetitionPattern = raw"R-L(\d+)...(\d+)".r.unanchored
    val repetitionPattern(start, finish) = instructions.take(1).head

    val hasJumpLine = instructions.find(_ contains "JL")
    val lineRange = hasJumpLine
      .map(_ => start.toInt to finish.toInt by 2)
      .getOrElse(start.toInt to finish.toInt)

    lineRange foreach { lineNumber =>
      instructions
        .drop(1)
        .dropRight(hasJumpLine.map(_ => 2).getOrElse(1))
        .map(_.split("->"))
        .filter(instructionParts => fieldValueFrom(lineNumber, instructionParts).nonEmpty)
        .map(instructionParts =>
          s"${fieldNameFrom(instructionParts)} -> ${fieldValueFrom(lineNumber, instructionParts)}"
        )
        .foreach(println)
    }

    def fieldValueFrom(lineNumber: Int, instructionParts: Array[String]): String =
      val regex = regexFor(DataType(fieldTypeFrom(instructionParts)))
      val tokens = documentLines(lineNumber - 1)
        .strip()
        .replaceFirst("\u00a0", "")
        .split(" ")
        .filter(_.nonEmpty)

      regex.findFirstIn(
        if tokens.isEmpty then ""
        else tokens(tokenNumberFrom(instructionParts(1), tokens.length))
          .strip()
          .replaceFirst("\u00a0", "")
      ).getOrElse("")

    def tokenNumberFrom(coordinate: String, numberOfTokens: Int): Int =
      val staticCoordinatePattern = raw"T(\d+)".r
      val dynamicCoordinatePattern = raw"T\(L-(\d+)\)".r

      coordinate.strip() match {
        case staticCoordinatePattern(token) => token.toInt - 1
        case dynamicCoordinatePattern(token) => numberOfTokens - token.toInt - 1
      }

object TemplateInterpreter:
  def apply(instructions: List[String]): TemplateInterpreter = SequentialInterpreter(instructions)