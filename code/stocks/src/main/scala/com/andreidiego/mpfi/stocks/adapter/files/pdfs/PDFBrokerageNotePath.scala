package com.andreidiego.mpfi.stocks.adapter.files.pdfs

import scala.annotation.experimental
import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPath

@experimental class PDFBrokerageNotePath[F[_]] private(val path: String) extends FileSystemPath[F](path: String)

@experimental object PDFBrokerageNotePath:
  import java.time.LocalDate
  import java.time.format.DateTimeFormatter
  import java.util.regex.Pattern
  import language.experimental.saferExceptions
  import scala.util.Try
  import FileSystemPath.Exceptions.*
  import Messages.*

  def from[F[_]](path: String): PDFBrokerageNotePath[F] throws FileSystemPath.Exceptions =
    FileSystemPath.from[F](path)
    if path.isNotPDF then
      throw UnexpectedContentValue(nonPDFFileSystemPath(path))
    else if path.fileNameHasLessThanThreeSections then
      throw UnexpectedContentValue(incompleteFileName(path))
    else if path.fileNameHasMoreThanThreeSections then
      throw UnexpectedContentValue(invalidFileNameStructure(path))
    else if path.fileNameMissesOperationsDescription then
      throw RequiredValueMissing(fileNameMissingOperationsDescription(path))
    else if path.fileNameOperationsDescriptionHasOnlyNumbers then
      throw UnexpectedContentValue(operationsDescriptionWithOnlyNumbers(path))
    else if path.fileNameOperationsDescriptionHasOnlyLetters then
      throw UnexpectedContentValue(operationsDescriptionWithOnlyLetters(path))
    else if path.fileNameMissesNoteNumber then
      throw RequiredValueMissing(fileNameMissingNoteNumber(path))
    else if path.fileNameNoteNumberIsNotANumber then
      throw UnexpectedContentValue(fileNameWithNonNumericNoteNumber(path))
    else if path.fileNameMissesTradingDate then
      throw RequiredValueMissing(fileNameMissingTradingDate(path))
    else if path.fileNameTradingDateIsInvalidForFormat then
      throw UnexpectedContentValue(fileNameWithInvalidTradingDate(path))
    else PDFBrokerageNotePath(path)

  object Messages:
    val nonPDFFileSystemPath: String => String = path => s"$path is not a PDF file."
    val incompleteFileName: String => String = path => s"Filename in $path does not present the required three sections (OperationsDescription, NoteNumber, and TradingDate) or, they're not delimited by ' - '."
    val invalidFileNameStructure: String => String = path => s"Filename in $path does not follow the naming convention of 'OperationsDescription - NoteNumber - TradingDate.pdf' (e.g.'sellVALE3 - 18174 - 28-10-2022.txt')."
    val fileNameMissingOperationsDescription: String => String = path => s"$path filename's first section (OperationsDescription) can't be blank."
    val operationsDescriptionWithOnlyNumbers: String => String = path => s"$path filename's first section (OperationsDescription) can't have only numbers. It is supposed to describe operations (as text) on Tickers (which are generally alphanumeric)."
    val operationsDescriptionWithOnlyLetters: String => String = path => s"$path filename's first section (OperationsDescription) can't have only letters. It is supposed to describe operations (as text) on Tickers (which are generally alphanumeric)."
    val fileNameMissingNoteNumber: String => String = path => s"$path filename's second section (NoteNumber) can't be blank."
    val fileNameWithNonNumericNoteNumber: String => String = path => s"$path filename's second section (NoteNumber) can only have numbers."
    val fileNameMissingTradingDate: String => String = path => s"$path filename's third section (TradingDate) can't be blank."
    val fileNameWithInvalidTradingDate: String => String = path => s"$path filename's does not represent a valid date in the format 'dd-MM-yyyy'."

  extension(path: String)
    private def isNotPDF: Boolean = !path.endsWith(".pdf")
    private def fileNameHasLessThanThreeSections: Boolean = fileName.length < 3
    private def fileNameHasMoreThanThreeSections: Boolean = fileName.length > 3
    private def fileNameMissesOperationsDescription: Boolean = operationsDescription.isBlank
    private def fileNameOperationsDescriptionHasOnlyNumbers: Boolean = operationsDescription.hasOnlyNumbers
    private def fileNameOperationsDescriptionHasOnlyLetters: Boolean = operationsDescription.hasOnlyLetters
    private def fileNameMissesNoteNumber: Boolean = noteNumber.isBlank
    private def fileNameNoteNumberIsNotANumber: Boolean = !noteNumber.hasOnlyNumbers
    private def fileNameMissesTradingDate: Boolean = tradingDate.isBlank
    private def fileNameTradingDateIsInvalidForFormat: Boolean =
      Try(LocalDate.parse(tradingDate, DateTimeFormatter.ofPattern("dd-MM-yyyy"))).isFailure
    private def fileName: Array[String] = pathSegments(pathSegments.length - 1).split("- | - | -")
    private def operationsDescription: String = fileName(0)
    private def hasOnlyNumbers: Boolean = raw"\d+".r.matches(path)
    private def hasOnlyLetters: Boolean = raw"[a-zA-Z]+".r.matches(path)
    private def noteNumber: String = fileName(1)
    private def tradingDate: String = fileName(2).replaceFirst(raw"\.[a-zA-Z0-9]{2,4}", "")
    private def pathSegments: Array[String] = path.split(s"[/${Pattern.quote("\\")}]")