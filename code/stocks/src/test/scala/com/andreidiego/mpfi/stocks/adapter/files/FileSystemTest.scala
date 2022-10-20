package com.andreidiego.mpfi.stocks.adapter.files

object FileSystemTest:
  import java.nio.file.Path
  import scala.util.Try
  import scala.util.Failure
  import cats.data.State

  enum ResourceType:
    case File, Folder

  type FileSystemState = Map[Path, ResourceType]
  type StateFileSystem[A] = State[FileSystemState, A]

  val emptyState: FileSystemState = Map()

  def apply[A](s: FileSystemState => (FileSystemState, A)): StateFileSystem[A] = State(s)

  // TODO Add law checking to this instance
  //noinspection NonAsciiCharacters
  class TestFileSystem extends FileSystem[StateFileSystem]:
    import java.nio.file.FileAlreadyExistsException
    import java.nio.file.NoSuchFileException
    import java.nio.file.DirectoryNotEmptyException
    import scala.util.Success
    import cats.syntax.apply.*
    import ResourceType.*

    override def createFile(path: Path): StateFileSystem[Try[Path]] = 
      exists(path).flatMap { exists =>
        if exists then FileSystemTest(fss ⇒ (fss, Failure(FileAlreadyExistsException(path.toString))))
        else FileSystemTest(fss ⇒ (fss + (path → File), Success(path)))
      }

    override def createFolder(path: Path): StateFileSystem[Try[Path]] = 
      exists(path).flatMap { exists =>
        if exists then FileSystemTest(fss ⇒ (fss, Failure(FileAlreadyExistsException(path.toString))))
        else FileSystemTest(fss ⇒ (fss + (path → Folder), Success(path)))
      }
      
    override def delete(path: Path, force: Boolean = false): StateFileSystem[Try[Path]] = FileSystemTest { fss ⇒ 
      (exists(path), isAFolder(path)).mapN { (exists, isAFolder) =>
        if !exists then (fss, Failure(NoSuchFileException(path.toString)))
        else 
          if isAFolder && /* !isEmpty */ fss.count((innerPath, _) => innerPath.startsWith(path)) > 1 then 
            if force then (
              /* deleteSubtree */ fss.filter((innerPath, _) => !innerPath.startsWith(path)), 
              Success(path)
            )
            else (fss, Failure(DirectoryNotEmptyException(path.toString)))
          else 
            (fss - path, Success(path))
      }.runA(fss).value
    }

    override def exists(path: Path): StateFileSystem[Boolean] = FileSystemTest(fss ⇒ (fss, fss.contains(path)))
    override def isAFile(path: Path): StateFileSystem[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(File)))
    override def isAFolder(path: Path): StateFileSystem[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(Folder)))  

  given FileSystem[StateFileSystem] = TestFileSystem()

  object FileSystemUOE extends TestFileSystem:
    override def createFile(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(UnsupportedOperationException())))
    override def createFolder(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(UnsupportedOperationException())))
    
  object FileSystemIOE extends TestFileSystem:
    import java.io.IOException

    override def createFile(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(IOException())))
    override def createFolder(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(IOException())))
    override def delete(path: Path, force: Boolean = false): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(IOException())))

  object FileSystemSE extends TestFileSystem:
    override def createFile(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(SecurityException())))
    override def createFolder(path: Path): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(SecurityException())))
    override def delete(path: Path, force: Boolean = false): StateFileSystem[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(SecurityException())))