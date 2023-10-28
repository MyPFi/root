package com.andreidiego.filesystem

import java.nio.file.Path
import scala.util.Try

trait FileSystem[F[_]]:
  def createFile(path: Path, content: String): F[Try[Path]]
  def createFolder(path: Path): F[Try[Path]]
  def delete(path: Path, force: Boolean = false): F[Try[Path]]
  def exists(path: Path): F[Boolean]
  def isAFile(path: Path): F[Boolean]
  def isAFolder(path: Path): F[Boolean]
  def contentsOf(path: Path): F[Try[LazyList[String]]]

object FileSystem:
  import java.lang.Exception as JavaException
  import cats.Id
  import FileSystemMessages.*

  enum Exception(message: String) extends JavaException(message):
    case ResourceNotFound(resource: Path)
    extends Exception(resourceNotFound(s"$resource"))

  def apply[F[_]](using F: FileSystem[F]): FileSystem[F] = F

// TODO Look into introducing laws and law testing with Discipline
  given FileSystem[Id] = RealFileSystem()
  private class RealFileSystem extends FileSystem[Id]:
    import java.nio.file.{DirectoryNotEmptyException, FileAlreadyExistsException, NoSuchFileException, Path}
    import scala.util.{Failure, Success}
    import cats.syntax.apply.*
    import cats.syntax.flatMap.*

    override def createFile(path: Path, content: String): Id[Try[Path]] =
      exists(path).flatMap { exists =>
        if exists then Failure(FileAlreadyExistsException(path.toString))
        else
          os.write(target = os.Path(path), data = content, createFolders = true)
          Success(path)
      }

    override def createFolder(path: Path): Id[Try[Path]] =
      exists(path).flatMap { exists =>
        if exists then Failure(FileAlreadyExistsException(path.toString))
        else
          os.makeDir(os.Path(path))
          Success(path)
      }

    override def delete(path: Path, force: Boolean = false): Id[Try[Path]] = Id {
      (exists(path), isAFolder(path), contentsOf(path)).mapN { (exists, isAFolder, contents) =>
        if !exists then Failure(NoSuchFileException(path.toString))
        else if isAFolder && contents.get.nonEmpty then
          if force then
            os.remove.all(os.Path(path))
            Success(path)
          else Failure(DirectoryNotEmptyException(path.toString))
        else
          os.remove(os.Path(path))
          Success(path)
      }
    }

    override def exists(path: Path): Id[Boolean] = os.exists(os.Path(path))

    override def isAFile(path: Path): Id[Boolean] = os.isFile(os.Path(path))

    override def isAFolder(path: Path): Id[Boolean] = os.isDir(os.Path(path))

    override def contentsOf(path: Path): Id[Try[LazyList[String]]] =
      (exists(path), isAFile(path)).mapN { (exists, isAFile) =>
        if exists then Success {
          if isAFile then os.read.lines(os.Path(path)).to(LazyList)
          //        if isAFile then os.read.lines.stream(os.Path(path))
          else os.walk(os.Path(path)).map(_.toString).to(LazyList)
          //        else os.list.stream(os.Path(path))
        } else Failure(FileSystem.Exception.ResourceNotFound(path))
      }

object FileSystemMessages:
  val resourceNotFound: String => String =
    path => s"Resource '${Path.of(path)}' cannot be found."