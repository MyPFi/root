package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.annotation.experimental
import scala.util.matching.Regex
import cats.Functor
import cats.syntax.functor.*

enum FileSystemPathException(message: String) extends Exception(message):
  case RequiredValueMissingException(message: String) extends FileSystemPathException(message)
  case UnexpectedContentValueException(message: String) extends FileSystemPathException(message)

@experimental class FileSystemPath[F[_]](path: String):
  import java.nio.file.Files
  import FileSystemPath.*

  def exists: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    FileSystem[F].exists(Path.of(path))

  def doesNotExist: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    exists.map(!_)

  def isAFile: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    FileSystem[F].isAFile(Path.of(path)).map(_ || path.hasExtension || path.doesNotEndWithSlash)

  def isNotAFile: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFile.map(!_)

  def isAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    FileSystem[F].isAFolder(Path.of(path)).map(_ || path.endsWithSlash)

  def isNotAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFolder.map(!_)

@experimental object FileSystemPath:
  import java.nio.file.InvalidPathException
  import language.experimental.saferExceptions
  import FileSystemPathMessages.*
  import FileSystemPathException.*

  type InteractsWithTheFileSystemAndReturns[A] = [F[_]] =>> FileSystem[F] ?=> Functor[F] ?=> F[A]

  def from[F[_]](path: String): FileSystemPath[F] throws FileSystemPathException =
    if path.isBlank then
      throw RequiredValueMissingException(fileSystemPathMissing) 
    else if !path.validated.isAbsolute then
      throw UnexpectedContentValueException(relativeFileSystemPathNotAllowed(path))
    else FileSystemPath[F](path)

  val fileExtensionRegex: Regex = """\.[^.\\/:*?"<>|\r\n]+$""".r
  val folderRegex: Regex = """[\\/]$""".r

  extension(path: String)
    private def validated: Path throws FileSystemPathException = try Path.of(path)
      catch case _: InvalidPathException => throw UnexpectedContentValueException(invalidFileSystemPath(path))
    private def hasExtension: Boolean = fileExtensionRegex.findFirstIn(path).isDefined
    private def endsWithSlash: Boolean = folderRegex.findFirstIn(path).isDefined
    private def doesNotEndWithSlash: Boolean = !endsWithSlash

object FileSystemPathMessages:
  val fileSystemPathMissing = "Path cannot be empty."
  val invalidFileSystemPath: String => String =
    path => s"$path does not represent a valid filesytem path."
  val relativeFileSystemPathNotAllowed: String => String =
    path => s"Relative filesystem paths (like '$path') are not allowed."