package com.andreidiego.mpfi.stocks.adapter.files.pdfs

import java.time.LocalDate
import java.util.Locale
import java.util.regex.Pattern
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.annotation.experimental

@experimental object PDFBrokerageNotePath:
  import language.experimental.saferExceptions
  import com.andreidiego.mpfi.stocks.adapter.files.{FileSystemPath, FileSystemPathException}
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathException.*
  import PDFBrokerageNotePathMessages.*

  def from[F[_]](path: String): FileSystemPath[F] throws FileSystemPathException =
    val f = FileSystemPath.from[F](path)
    if path.isNotPDF then
      throw UnexpectedContentValueException(nonPDFFileSystemPath(path))
    else if path.fileNameDoesNotPresentTheRequiredThreeSections then
      throw UnexpectedContentValueException(incompleteFileName(path))
    else if path.fileNameMissesOperationsDescription then
      throw RequiredValueMissingException(fileNameMissingOperationsDescription(path))
    else if path.fileNameOperationsDescriptionHasOnlyNumbers then
      throw UnexpectedContentValueException(operationsDescriptionWithOnlyNumbers(path))
    else if path.fileNameOperationsDescriptionHasOnlyLetters then
      throw UnexpectedContentValueException(operationsDescriptionWithOnlyLetters(path))
    else if path.fileNameMissesNoteNumber then
      throw RequiredValueMissingException(fileNameMissingNoteNumber(path))
    else if path.fileNameNoteNumberIsNotANumber then
      throw UnexpectedContentValueException(fileNameWithNonNumericNoteNumber(path))
    else if path.fileNameMissesTradingDate then
      throw RequiredValueMissingException(fileNameMissingTradingDate(path))
    else if path.fileNameTradingDateIsInvalidForFormat then
      throw UnexpectedContentValueException(fileNameWithInvalidTradingDate(path))
    else f

  extension(path: String)
    private def isNotPDF: Boolean = !path.endsWith(".pdf")
    private def pathSegments: Array[String] = path.split(s"[/${Pattern.quote("\\")}]")
    private def fileName: Array[String] = pathSegments(pathSegments.length - 1).split("- | - | -")
    private def fileNameDoesNotPresentTheRequiredThreeSections: Boolean = fileName.length < 3
    private def operationsDescription: String = fileName(0)
    private def fileNameMissesOperationsDescription: Boolean = operationsDescription.isBlank
    private def fileNameOperationsDescriptionHasOnlyNumbers: Boolean = operationsDescription.hasOnlyNumbers
    private def fileNameOperationsDescriptionHasOnlyLetters: Boolean = operationsDescription.hasOnlyLetters
    private def noteNumber: String = fileName(1)
    private def fileNameMissesNoteNumber: Boolean = noteNumber.isBlank
    private def fileNameNoteNumberIsNotANumber: Boolean = !noteNumber.hasOnlyNumbers
    private def tradingDate: String = fileName(2).replaceFirst(raw"\.[a-zA-Z0-9]{2,4}", "")
    private def fileNameMissesTradingDate: Boolean = tradingDate.isBlank
    private def fileNameTradingDateIsInvalidForFormat: Boolean =
      Try(LocalDate.parse(tradingDate, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).isFailure

  extension(string: String)
    private def hasOnlyNumbers: Boolean = raw"\d+".r.matches(string)
    private def hasOnlyLetters: Boolean = raw"[a-zA-Z]+".r.matches(string)

object PDFBrokerageNotePathMessages:
  val nonPDFFileSystemPath: String => String = path => s"$path is not a PDF file."
  val incompleteFileName: String => String = path => s"Filename in $path does not present the required three sections (OperationsDescription, NoteNumber, and TradingDate) or, they're not delimited by ' - '."
  val fileNameMissingOperationsDescription: String => String = path => s"$path filename's first section (OperationsDescription) can't be blank."
  val operationsDescriptionWithOnlyNumbers: String => String = path => s"$path filename's first section (OperationsDescription) can't have only numbers. It is supposed to describe operations (as text) on Tickers (which are generally alphanumeric)."
  val operationsDescriptionWithOnlyLetters: String => String = path => s"$path filename's first section (OperationsDescription) can't have only letters. It is supposed to describe operations (as text) on Tickers (which are generally alphanumeric)."
  val fileNameMissingNoteNumber: String => String = path => s"$path filename's second section (NoteNumber) can't be blank."
  val fileNameWithNonNumericNoteNumber: String => String = path => s"$path filename's second section (NoteNumber) can only have numbers."
  val fileNameMissingTradingDate: String => String = path => s"$path filename's third section (TradingDate) can't be blank."
  val fileNameWithInvalidTradingDate: String => String = path => s"$path filename's does not represent a valid date in the format 'dd-MM-yyyy'."