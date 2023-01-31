package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.annotation.experimental
import cats.Monad

// TODO Constructor must be private
@experimental class FileSystemPath[F[_]] protected(path: String):
  import scala.util.Try
  import scala.util.Success
  import scala.util.Failure
  import cats.syntax.functor.*
  import cats.syntax.apply.*
  import cats.syntax.flatMap.*
  import FileSystemPath.*
  import Exceptions.*
  import Sorters.{noSorting, sortedAlphabetically}

  /* FIXME In hindsight, FileSystemPath should have less logic. Mush of the logic here should
   *  be in the underlying FileSystem and some of it doesn't even make a lot of sense:
   *    1) When trying to create a resource:
   *      - 'FileSystemPath' would take a parameter indicating the type of resource
   *        to be created instead of trying to guess it from the string path;
   *      - If it already exists, 'FileSystem' would throw an 'ResourceAlreadyExists'
   *        exception (indicating the type of the existing resource) that would simply
   *        pass through 'FileSystemPath' up the call chain. Simply ignoring that the
   *        resource already exists may cause confusion, even more so when the resource
   *        already has some content. Suppose a user tries to create a folder and that
   *        folder already exists and has contents. Then he immediately decides to delete
   *        it and the system says it can't delete it because it already has contents or,
   *        even worse, the user inadvertently uses the 'force' flag when deleting it and
   *        now all the contents of the folder goes away without notice. Both cases are odd
   *        and applies to files as well. Trying to cope with this situation would add
   *        unnecessary complexity to the design.
   *    2) When trying to delete a resource that doesn't exist, simply ignoring it would pose
   *    a lesser problem than above and could even streamline API usage but, in the name of
   *    consistence ond of the user's freedom to decide, 'FileSystem' would throw a 'ResourceNotFound'
   *    exception that would be passed through 'FileSystemPath' up the call chain as well.
   *    3) When trying to figure out the type of the resource (if a 'File' or a 'Folder'),
   *    that decision could only be made if the resource already exists (instead of trying
   *    to guess it from the string path). If the resource does not exists, 'FileSystem'
   *    would also throw a 'ResourceNotFound' exception that would again be passed through
   *    'FileSystemPath' up the call chain.
   *    4) When trying to get the contents of the resource, this also would only be possible
   *    if the resource exists. If the resource
   *      - Does not exist:
   *        - 'FileSystem' would throw a 'ResourceNotFound' exception that would be passed
   *        through 'FileSystemPath' up the call chain.
   *      - Exists and it is
   *        - Empty, an empty stream will be returned by the 'FileSystem' and passed through
   *        the 'FileSystemPath'
   *        - A non-empty
   *         - Folder:
   *           Its contents (the paths to the files and folders under it) would be streamed
   *           from 'FileSystem' as 'String's and in no particular order. It would be left
   *           to 'FileSystemPath' to put them in the default alphabetical order of the inner
   *           resource's full paths, having to, obviously, exhaust the stream in order to
   *           achieve that (There's a bit of a controversy here in relation to the natural
   *           tension between streaming and ordering so, this may change again in the future
   *           and will be proved in the field in order to figure out what is best).
   *         - File:
   *           Its contents (the lines of the file) would be streamed from 'FileSystem' as
   *           'String's in the order they appear in the file. It would then be passed up,
   *           still as a stream of 'String's, by the 'FileSystemPath'.
   *    5) When trying to overwrite the content of a file, if the resource
   *      - Already exists and it is a
   *        - Folder:
   *          A 'ResourceWithConflictingTypeAlreadyExists' will be thrown by the 'FileSystemPath'. ✓
   *        - File:
   *          The file will be deleted and recreated with the new content by the 'FileSystemPath'.
   *      - Doesn't exist, it will be created with the given content by the 'FileSystemPath'.
   */
  def create: InteractsWithTheFileSystemAndReturns[Try[Path]][F] =
    val canCreate = (exists, FileSystem[F].isAFile(Path.of(path)), FileSystem[F].isAFolder(Path.of(path)))
      .mapN { (exists, isAFile, isAFolder) =>
        if exists && isAFile && path.endsWithSlash then
          Failure(ResourceWithConflictingTypeAlreadyExists(Path.of(path), "Folder", "File"))
        else if exists && isAFolder && path.doesNotEndWithSlash then
          Failure(ResourceWithConflictingTypeAlreadyExists(Path.of(path), "File", "Folder"))
        else Success(Path.of(path))
      }

    canCreate.flatMap { okToCreate =>
      if okToCreate.isFailure then canCreate
      else doesNotExist.flatMap{ doesNotExist =>
        if doesNotExist then
          if path.endsWithSlash then FileSystem[F].createFolder(Path.of(path))
          else FileSystem[F].createFile(Path.of(path), "")
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
      (exists && isAFile) || (doesNotExist && (path.hasExtension || path.doesNotEndWithSlash))
    }

  def isNotAFile: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFile.map(!_)

  def isAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    (exists, FileSystem[F].isAFolder(Path.of(path)), doesNotExist).mapN{ (exists, isAFolder, doesNotExist) =>
      (exists && isAFolder) || (doesNotExist && path.endsWithSlash)
    }

  def isNotAFolder: InteractsWithTheFileSystemAndReturns[Boolean][F] =
    isAFolder.map(!_)

  def contents: InteractsWithTheFileSystemAndReturns[Try[LazyList[String]]][F] = isAFolder.flatMap { isAFolder ⇒
    if isAFolder then contents(sortedAlphabetically)
    else contents(noSorting)
  }

  def contents(sortedWith: (String, String) ⇒ Boolean): InteractsWithTheFileSystemAndReturns[Try[LazyList[String]]][F] =
    FileSystem[F].contentsOf(Path.of(path)).map(_.map(_.sortWith(sortedWith)))

  def overwriteWith(aText: String): InteractsWithTheFileSystemAndReturns[Try[Path]][F] =
    exists.flatMap { resourceExists ⇒ {
      isAFolder.flatMap { resourceIsAFolder ⇒ {
        if resourceExists then
          if resourceIsAFolder then summon[Monad[F]].pure(
            Failure(ResourceWithConflictingTypeAlreadyExists(Path.of(path), "File", "Folder"))
          ) else
            delete(true).flatMap{ deleted ⇒
              if deleted.isSuccess then FileSystem[F].createFile(Path.of(path), aText)
              else summon[Monad[F]].pure(deleted)
            }
        else FileSystem[F].createFile(Path.of(path), aText)
      }}
    }}

object FileSystemPath:
  import language.experimental.saferExceptions
  import scala.util.matching.Regex
  import Messages.*

  type InteractsWithTheFileSystemAndReturns[A] = [F[_]] =>> FileSystem[F] ?=> Monad[F] ?=> F[A]

  val fileExtensionRegex: Regex = """\.[^.\\/:*?"<>|\r\n]+$""".r
  val folderRegex: Regex = """[\\/]$""".r

  @experimental def from[F[_]](path: String): FileSystemPath[F] throws Exceptions =
    if path.isBlank then
      throw Exceptions.RequiredValueMissing(fileSystemPathMissing)
    else if !path.validated.isAbsolute then
      throw Exceptions.UnexpectedContentValue(relativeFileSystemPathNotAllowed(path))
    else FileSystemPath[F](path)

  trait Sorter extends ((String, String) ⇒ Boolean)

  object Sorters:
    val sortedAlphabetically: Sorter = (line1, line2) ⇒ line1.compareToIgnoreCase(line2) < 0
    val noSorting: Sorter = (line1, line2) ⇒ line1.compareToIgnoreCase(line2) == 0

  enum Exceptions(message: String) extends Exception(message):
    @experimental case RequiredValueMissing(message: String) extends Exceptions(message)
    @experimental case UnexpectedContentValue(message: String) extends Exceptions(message)
    /*
     FIXME I'd like to use ResourceType as the type for both parameters here but, when I bring the
      ResourceType enum out of the FileSystemTest object into the production scope, the
      given FileSystem[FileSystemTest] = TestFileSystem() can't be found and application won't compile
     TODO Use the type trick to prevent the caller from inverting the order of the parameters 'desiredType' and 'currentType'
    */
    @experimental case ResourceWithConflictingTypeAlreadyExists(resource: Path, desiredType: String, currentType: String)
      extends Exceptions(resourceCannotBeCreated(resource.toString, desiredType, currentType))

  object Messages:
    val fileSystemPathMissing = "Path cannot be empty."
    val invalidFileSystemPath: String => String =
      path => s"$path does not represent a valid filesystem path."
    val relativeFileSystemPathNotAllowed: String => String =
      path => s"Relative filesystem paths (like '$path') are not allowed."
    val resourceCannotBeCreated: (String, String, String) ⇒ String =
      (resource, desiredType, currentType) ⇒ s"Cannot create '$resource' as a '$desiredType' since it already exists as a '$currentType'."

  extension(path: String)
    private def validated: Path throws FileSystemPath.Exceptions =
      import java.nio.file.InvalidPathException

      try Path.of(path)
      catch case _: InvalidPathException => throw Exceptions.UnexpectedContentValue(invalidFileSystemPath(path))

    private def hasExtension: Boolean = fileExtensionRegex.findFirstIn(path).isDefined
    private def endsWithSlash: Boolean = folderRegex.findFirstIn(path).isDefined
    private def doesNotEndWithSlash: Boolean = !endsWithSlash