package com.andreidiego.mpfi.stocks.adapter.files

import scala.annotation.experimental

@experimental object ExtractionGuideSpecPath:
  import java.util.regex.Pattern
  import language.experimental.saferExceptions
  import com.andreidiego.mpfi.stocks.adapter.files.{FileSystemPath, FileSystemPathException}
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathException.*
  import ExtractionGuideSpecPathMessages.*

  def from[F[_]](path: String): FileSystemPath[F] throws FileSystemPathException =
    val f = FileSystemPath.from[F](path)
    if path.isNotTXT then
      throw UnexpectedContentValueException(nonTXTFileSystemPath(path))
    else if path.fileNameHasLessThanThreeSections then
      throw UnexpectedContentValueException(incompleteFileName(path))
    else if path.fileNameHasMoreThanThreeSections then
      throw UnexpectedContentValueException(invalidFileNameStructure(path))
    else if path.fileNameMissesDocumentType then
      throw RequiredValueMissingException(fileNameMissingDocumentType(path))
    else if path.fileNameDocumentTypeHasInvalidCharacters then
      throw UnexpectedContentValueException(documentTypeWithInvalidCharacters(path))
    else if path.fileNameMissesDocumentIssuer then
      throw RequiredValueMissingException(fileNameMissingDocumentIssuer(path))
    else if path.fileNameMissesDocumentVersion then
      throw RequiredValueMissingException(fileNameMissingDocumentVersion(path))
    else if path.fileNameHasInvalidDocumentVersion then
      throw UnexpectedContentValueException(fileNameWithInvalidDocumentVersion(path))
    else f

  extension(path: String)
    private def isNotTXT: Boolean = !path.endsWith(".txt")
    private def pathSegments: Array[String] = path.split(s"[/${Pattern.quote("\\")}]")
    private def fileName: Array[String] = pathSegments(pathSegments.length - 1).split("-")
    private def fileNameHasLessThanThreeSections: Boolean = fileName.length < 3
    private def fileNameHasMoreThanThreeSections: Boolean = fileName.length > 3
    private def documentType: String = fileName(0)
    private def fileNameMissesDocumentType: Boolean = documentType.isBlank
    private def fileNameDocumentTypeHasInvalidCharacters: Boolean = documentType.hasMoreThanOnlyLettersAndUnderscore
    private def documentIssuer: String = fileName(1)
    private def fileNameMissesDocumentIssuer: Boolean = documentIssuer.isBlank
    private def documentVersion: String = fileName(2).replaceFirst(raw"\.[a-zA-Z0-9]{2,4}", "")
    private def fileNameMissesDocumentVersion: Boolean = documentVersion.isBlank
    private def fileNameHasInvalidDocumentVersion: Boolean = documentVersion.isNotAValidVersionTag

  extension(string: String)
    private def hasMoreThanOnlyLettersAndUnderscore: Boolean = raw"[^a-zA-Z_]+".r.unanchored.matches(string)
    private def isNotAValidVersionTag: Boolean = !raw"[vV]\d+".r.matches(string)

object ExtractionGuideSpecPathMessages:
  val nonTXTFileSystemPath: String => String = path => s"$path is not a TXT file."
  val incompleteFileName: String => String = path => s"Filename in $path does not present the required three sections (DocumentType, DocumentIssuer, and DocumentVersion) or, they're not delimited by '-'."
  val invalidFileNameStructure: String => String = path => s"Filename in $path does not follow the naming convention of 'DocumentType-DocumentIssuer-DocumentVersion.txt' (e.g.'brokerage_note-best_broker-v1.txt')."
  val fileNameMissingDocumentType: String => String = path => s"$path filename's first section (DocumentType) can't be blank."
  val documentTypeWithInvalidCharacters: String => String = path => s"$path filename's first section (DocumentType) can only have letters and the '_' sign."
  val fileNameMissingDocumentIssuer: String => String = path => s"$path filename's second section (DocumentIssuer) can't be blank."
  val fileNameMissingDocumentVersion: String => String = path => s"$path filename's third section (DocumentVersion) can't be blank."
  val fileNameWithInvalidDocumentVersion: String => String = path => s"$path filename's third section (DocumentVersion) must be comprised of the letter 'v' (lower or uppercase) followed by a number as in 'v1' or 'V13'."