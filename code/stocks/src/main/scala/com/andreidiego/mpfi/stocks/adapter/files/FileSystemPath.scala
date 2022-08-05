package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.annotation.experimental
import cats.Monad
import FileSystemPathException.*

enum FileSystemPathException(message: String) extends Exception(message):
  case RequiredValueMissingException(message: String) extends FileSystemPathException(message)
  case UnexpectedContentValueException(message: String) extends FileSystemPathException(message)
  /* 
   FIXME I'd like to use ResourceType as the type for both parameters here but, when I bring the
    ResourceType enum out of the FileSystemTest object into the production scope, the 
    given FileSystem[FileSystemTest] = TestFileSystem() can't be found and application won't compile
   TODO Use the type trick to prevent the caller from inverting the order of the parameters 'desiredType' and 'currentType'
  */
  case ResourceWithConflictingTypeAlreadyExistsException(resource: Path, desiredType: String, currentType: String) 
    extends FileSystemPathException(s"Cannot create $resource as a $desiredType since it already exists as a $currentType.")

@experimental class FileSystemPath[F[_]](path: String):
  import scala.util.Try
  import scala.util.Success
  import scala.util.Failure
  import cats.syntax.functor.*
  import cats.syntax.apply.*
  import cats.syntax.flatMap.*
  import FileSystemPath.*

  def create: InteractsWithTheFileSystemAndReturns[Try[Path]][F] =
    val canCreate = (exists, FileSystem[F].isAFile(Path.of(path)), FileSystem[F].isAFolder(Path.of(path)))
      .mapN { (exists, isAFile, isAFolder) =>
        if exists && isAFile && path.endsWithSlash then Failure(ResourceWithConflictingTypeAlreadyExistsException(Path.of(path), "Folder", "File"))
        else if exists && isAFolder && path.doesNotEndWithSlash then Failure(ResourceWithConflictingTypeAlreadyExistsException(Path.of(path), "File", "Folder"))
        else Success(Path.of(path))
      }

    canCreate.flatMap { okToCreate => 
      if okToCreate.isFailure then canCreate
      else doesNotExist.flatMap{ doesNotExist => 
        if doesNotExist then 
          if path.endsWithSlash then FileSystem[F].createFolder(Path.of(path)) 
          else FileSystem[F].createFile(Path.of(path))
        else summon[Monad[F]].pure(Success(Path.of(path)))
      }
    } 

  // TODO Default parameters don't work as expected for methods that return functions
  def delete(force: Boolean = false): InteractsWithTheFileSystemAndReturns[Try[Path]][F] = 
    exists.flatMap { exists => 
      if exists then FileSystem[F].delete(Path.of(path), force)
      else summon[Monad[F]].pure(Success(Path.of(path)))
    }

  def exists: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    FileSystem[F].exists(Path.of(path))

  def doesNotExist: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    exists.map(!_)

  def isAFile: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    (exists, FileSystem[F].isAFile(Path.of(path)), doesNotExist).mapN{ (exists, isAFile, doesNotExist) =>
      ((exists && isAFile) || (doesNotExist && (path.hasExtension || path.doesNotEndWithSlash)))
    }

  def isNotAFile: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFile.map(!_)

  def isAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    (exists, FileSystem[F].isAFolder(Path.of(path)), doesNotExist).mapN{ (exists, isAFolder, doesNotExist) =>
      ((exists && isAFolder) || (doesNotExist && path.endsWithSlash))
    }

  def isNotAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFolder.map(!_)

@experimental object FileSystemPath:
  import language.experimental.saferExceptions
  import scala.util.matching.Regex
  import FileSystemPathMessages.*

  type InteractsWithTheFileSystemAndReturns[A] = [F[_]] =>> FileSystem[F] ?=> Monad[F] ?=> F[A]

  def from[F[_]](path: String): FileSystemPath[F] throws FileSystemPathException =
    if path.isBlank then
      throw RequiredValueMissingException(fileSystemPathMissing) 
    else if !path.validated.isAbsolute then
      throw UnexpectedContentValueException(relativeFileSystemPathNotAllowed(path))
    else FileSystemPath(path)

  val fileExtensionRegex: Regex = """\.[^.\\/:*?"<>|\r\n]+$""".r
  val folderRegex: Regex = """[\\/]$""".r

  extension(path: String)
    private def validated: Path throws FileSystemPathException =
      import java.nio.file.InvalidPathException

      try Path.of(path)
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