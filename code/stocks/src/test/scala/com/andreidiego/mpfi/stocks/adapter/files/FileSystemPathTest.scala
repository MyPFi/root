package com.andreidiego.mpfi.stocks.adapter.files

import java.io.IOException
import java.nio.file.Path
import scala.util.Try
import scala.annotation.experimental
import unsafeExceptions.canThrowAny
import org.scalatest.TryValues.*
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture
import org.scalatest.matchers.should.Matchers.*
import FileSystemTest.FileSystemTest
import FileSystemTest.emptyState

@experimental 
class FileSystemPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import language.deprecated.symbolLiterals
  import FileSystemPathMessages.*
  import FileSystemPathException.*
  import FileSystemPathTest.*
  import FileSystemTest.{FileSystemUOE, FileSystemIOE, FileSystemSE}

  "A 'FileSystemPath' should" - {
    "be built from a string representing a well-formed file system path." in { _ =>
      val fileSystemPath: String = os.home.toString

      "FileSystemPath.from(fileSystemPath)" should compile
    }
    "fail to be built when given" - {
      "an empty string." in { _ =>
        the [RequiredValueMissingException] thrownBy FileSystemPath.from[FileSystemTest]("") should have {
          'message (fileSystemPathMissing)
        }
      }
      "a relative file system path." in { _ =>
        val relativePath = "folder/file.ext"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from[FileSystemTest](relativePath) should have {
          'message (relativeFileSystemPathNotAllowed(relativePath))
        }
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files in Linux  - although I couldn't find an authoritative
       source for what is acceptable and what is not.
      */
      "a ill-formed file system path." in { _ =>
        val illFormedPath = s"${os.home}/?"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from[FileSystemTest](illFormedPath) should have {
          'message (invalidFileSystemPath(illFormedPath))
        }
      }
    }
    "when given a non-empty, well-formed, absolute file system path" - { 
      "be able to" - { 
        "tell if the file/folder it represents" - {
          "exists" in { configMap =>
            val fileName = "FileSystemPathTest-Exists.txt"

            assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
              path => FileSystemPath.from[FileSystemTest](path).exists,
              path => !FileSystemPath.from[FileSystemTest](path).doesNotExist
            )
          }
          "or not." in { configMap =>
            val file = "FileSystemPathTest-Exists.txt"

            assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
              path => FileSystemPath.from[FileSystemTest](path).doesNotExist,
              path => !FileSystemPath.from[FileSystemTest](path).exists
            )
          }
          "is a file, as long as" - { 
            "it exists as a file, independent of" - {
              "an extension being" - {
                "present" in { configMap =>
                  val fileName = "FileSystemPathTest-Exists.txt"

                  assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFolder
                  )
                }
                "or not." in { configMap =>
                  val fileName = "FileSystemPathTest-Exists"

                  assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFolder
                  )
                }
              }
              "it ending with" - {
                "'/' or '\\'" in { configMap =>
                  val fileName = "FileSystemPathTest-Exists/"

                  assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFolder
                  )
                }
                "or not." in { configMap =>
                  val fileName = "FileSystemPathTest-Exists"

                  assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFile,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFolder
                  )
                }
              }
            }
            "if it doesn't exist, it either ends with" - {
              "an extension" in { configMap =>
                val file = "FileSystemPathTest-Exists.txt"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => FileSystemPath.from[FileSystemTest](path).isAFile,
                  path => !FileSystemPath.from[FileSystemTest](path).isNotAFile
                )
              }
              "or, something different than '/' and '\\'" in { configMap =>
                val file = "FileSystemPathTest-Exists"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => FileSystemPath.from[FileSystemTest](path).isAFile,
                  path => !FileSystemPath.from[FileSystemTest](path).isNotAFile
                )
              }
            }
          }
          "or, a folder, as long as" - {
            "it exists as a folder, independent of" - {
              "being finished in" - { 
                "'/' or '\\'" in { configMap =>
                  val folderName = "folder/"

                  assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFile
                  )
                }
                "or not." in { configMap =>
                  val folderName = "folder"

                  assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFile
                  )
                }
              }
              "or, having" - { 
                "an extension" in { configMap =>
                  val folderName = "folder.hid"

                  assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFile
                  )
                }
                "or not." in { configMap =>
                  val folderName = "folder"

                  assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                    path => FileSystemPath.from[FileSystemTest](path).isAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isNotAFolder,
                    path => !FileSystemPath.from[FileSystemTest](path).isAFile,
                    path => FileSystemPath.from[FileSystemTest](path).isNotAFile,
                  )
                }
              }
            }
            "if it doesn't exist, it ends with a '/' or '\\'" in { configMap =>
              val folder = "folder/"

              assertWithNonExisting(folder)(configMap.getRequired("targetDir"))( 
                path => FileSystemPath.from[FileSystemTest](path).isAFolder,
                path => !FileSystemPath.from[FileSystemTest](path).isNotAFolder
              )
            }
          }
        }
        "create the underlying resource it represents, when the resource does not yet exists, be it a" - {
          "file." in { configMap =>
            assertResourceCreated("FileSystemPathTest.txt", _.isAFile)(configMap.getRequired("targetDir"))
          }
          "or, a folder." in { configMap =>
            assertResourceCreated("folder/", _.isAFolder)(configMap.getRequired("targetDir"))
          }
        }
        "silent return, when asked to create the underlying resource it represents, if the resource already exists, be it a" - {
          "file." in { configMap =>
            val file = "FileSystemPathTest.txt"
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"
            
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
            
            { for {
                _ <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                resource <- fileSystemPath.create
                result = resource.success.value should be(Path.of(pathString))
              } yield result
            }.run(emptyState).value
          }
          "or, a folder." in { configMap =>
            val folder = "folder/"
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"
            
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
            
            { for {
                _ <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                resource <- fileSystemPath.create
                result = resource.success.value should be(Path.of(pathString))
              } yield result
            }.run(emptyState).value
          }
        }
        "report any failures in creating the underlying resource, be it" - {
          "an UnsupportedOperationException when creating a" - {
            "file" in { configMap =>
              val file = "FileSystemPathTest.txt"
              val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"
                
              val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
              
              { for {
                  resource <- fileSystemPath.create(using FileSystemUOE)
                  result = resource.failure.exception shouldBe a [UnsupportedOperationException]
                } yield result
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folder = "folder/"
              val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"
                
              val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)

              { 
                for {
                  resource <- fileSystemPath.create(using FileSystemUOE)
                  result = resource.failure.exception shouldBe a [UnsupportedOperationException]
                } yield result
              }.run(emptyState).value
            }
          }
          "an IOException when creating a" - {
            "file" in { configMap =>
              val file = "FileSystemPathTest.txt"
              val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"
                
              val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
              
              { for {
                  resource <- fileSystemPath.create(using FileSystemIOE)
                  result = resource.failure.exception shouldBe a [IOException]
                } yield result
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folder = "folder/"
              val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"
                
              val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)

              { 
                for {
                  resource <- fileSystemPath.create(using FileSystemIOE)
                  result = resource.failure.exception shouldBe a [IOException]
                } yield result
              }.run(emptyState).value
            }
          }
        }
        "a SecurityException when creating a" - {
          "file" in { configMap =>
            val file = "FileSystemPathTest.txt"
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"
              
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
            
            { for {
                resource <- fileSystemPath.create(using FileSystemSE)
                result = resource.failure.exception shouldBe a [SecurityException]
              } yield result
            }.run(emptyState).value
          }
          "or, a folder." in { configMap =>
            val folder = "folder/"
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"
              
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)

            { 
              for {
                resource <- fileSystemPath.create(using FileSystemSE)
                result = resource.failure.exception shouldBe a [SecurityException]
              } yield result
            }.run(emptyState).value
          }
        }
        "or, a ResourceWithConflictingTypeAlreadyExistsException when trying to create a" - {
          "file and a folder with that name already exists" in { configMap =>
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/folder"
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
            { 
              for {
                _ <- assertResourceCreated("folder/", _.isAFolder)(configMap.getRequired("targetDir"))
                resource <- fileSystemPath.create
                exception = resource.failure.exception 
                result = exception shouldBe a [ResourceWithConflictingTypeAlreadyExistsException]
                message = exception should have message s"Cannot create ${pathString.replace("/", "\\")} as a File since it already exists as a Folder."
              } yield (result, message)
            }.run(emptyState).value
          }
          "folder and a file with that name already exists" in { configMap =>
            val pathString = s"${configMap.getRequired[String]("targetDir")}/test-files/file/"
            val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)
            { 
              for {
                _ <- assertResourceCreated("file", _.isAFile)(configMap.getRequired("targetDir"))
                resource <- fileSystemPath.create
                exception = resource.failure.exception 
                result = exception shouldBe a [ResourceWithConflictingTypeAlreadyExistsException]
                message = exception should have message s"Cannot create ${pathString.replace("/", "\\").dropRight(1)} as a Folder since it already exists as a File."
              } yield (result, message)
            }.run(emptyState).value
          }
        }
      }
    }
  }

object FileSystemPathTest:
  import org.scalatest.Assertions.assert
  import org.scalatest.compatible.Assertion

  private def assertWithExistingFile(fileName: String)(buildTarget: String)(
    assertions: String => FileSystemTest[Boolean]*
  ): Unit = 
    assertWithExisting(fileName, buildTarget, FileSystem[FileSystemTest].createFile(_), assertions: _*)

  private def assertWithExistingFolder(folderName: String)(buildTarget: String)(
    assertions: String => FileSystemTest[Boolean]*
  ): Unit = 
    assertWithExisting(folderName, buildTarget, FileSystem[FileSystemTest].createFolder(_), assertions: _*)
    
  private def assertWithExisting(
    resourceName: String, 
    buildTarget: String, 
    createResourceAt: Path => FileSystemTest[Try[Path]],
    assertions: String => FileSystemTest[Boolean]*
  ): Unit =
    val fileSystem = FileSystem[FileSystemTest] 
    val currentFolder = Path.of(s"$buildTarget/test-files")
    val path = Path.of(s"$currentFolder/$resourceName")

    val createResources = for {
      _ <- fileSystem.createFolder(currentFolder)
      _ <- createResourceAt(path)
      _ <- FileSystemTest(fss => (fss, assertions.map(_(path.toString).map(assert(_)).run(fss).value)))
      _ <- fileSystem.delete(path)
      _ <- fileSystem.delete(currentFolder)
    } yield()

    createResources.run(emptyState).value
    
  private def assertWithNonExisting(resourceName: String)(buildTarget: String)(
    assertions: String => FileSystemTest[Boolean]*
  ): Unit = 
    val currentFolder = s"$buildTarget/test-files"
    val path = s"$currentFolder/$resourceName"

    assertions.foreach(_(path).map(assert(_)).run(emptyState).value)

  private def assertResourceCreated(
    resourceName: String,
    resourceType: FileSystemPath[FileSystemTest] => FileSystemTest[Boolean]
  )(buildTarget: String): FileSystemTest[(Assertion, Assertion, Assertion, Assertion)] =
    import cats.Functor
    import cats.syntax.functor.*

    def assertThat[F[_]: Functor](fileSystemQuery: F[Boolean]): F[Assertion] =
      fileSystemQuery.map(assert(_))

    val currentFolder = s"$buildTarget/test-files"
    val pathString = s"$currentFolder/$resourceName"

    val fileSystemPath = FileSystemPath.from[FileSystemTest](pathString)

    val assertions = for {
      doesNotExist <- assertThat(fileSystemPath.doesNotExist)
      created <- fileSystemPath.create.map(_.success.value should be (Path.of(pathString)))
      exists <- assertThat(fileSystemPath.exists)
      resourceTypeMatches <- assertThat(resourceType(fileSystemPath))
    } yield (doesNotExist, created, exists, resourceTypeMatches)

    assertions.run(emptyState).value
    assertions

  extension(fileSystemTest: FileSystemTest[Boolean])
    private def unary_! : FileSystemTest[Boolean] = fileSystemTest.map(!_)

object FileSystemTest:
  import scala.util.Failure
  import cats.data.State

  enum ResourceType:
    case File, Folder

  type FileSystemState = Map[Path, ResourceType]
  type FileSystemTest[A] = State[FileSystemState, A]

  val emptyState: FileSystemState = Map()

  def apply[A](s: FileSystemState => (FileSystemState, A)): FileSystemTest[A] = State(s)

  // TODO Add law checking to this instance
  //noinspection NonAsciiCharacters
  class TestFileSystem extends FileSystem[FileSystemTest]:
    import scala.util.Success
    import ResourceType.*

    // TODO Swallow 'FileAlreadyExistsException'
    override def createFile(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss + (path → File), Success(path)))
    // TODO Swallow 'FileAlreadyExistsException'
    override def createFolder(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss + (path → Folder), Success(path)))
    /*
      TODO Swallow 'NoSuchFileException'
      TODO Take further action (delete the folder's contents) on 'DirectoryNotEmptyException' if force == true
      TODO Fail with 'DirectoryNotEmptyException' if folder not empty and 'force == false'
    */
    override def delete(path: Path, force: Boolean = false): FileSystemTest[Unit] = FileSystemTest(fss ⇒ (fss - path, ()))
    override def exists(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.contains(path)))
    override def isAFile(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(File)))
    override def isAFolder(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(Folder)))  
  
  given FileSystem[FileSystemTest] = TestFileSystem()

  object FileSystemUOE extends TestFileSystem:
    override def createFile(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(UnsupportedOperationException())))
    override def createFolder(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(UnsupportedOperationException())))
    
  object FileSystemIOE extends TestFileSystem:
    override def createFile(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(IOException())))
    override def createFolder(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(IOException())))

  object FileSystemSE extends TestFileSystem:
    override def createFile(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(SecurityException())))
    override def createFolder(path: Path): FileSystemTest[Try[Path]] = FileSystemTest(fss ⇒ (fss, Failure(SecurityException())))