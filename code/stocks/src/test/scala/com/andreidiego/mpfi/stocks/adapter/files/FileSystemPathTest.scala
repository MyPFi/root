package com.andreidiego.mpfi.stocks.adapter.files

import scala.annotation.experimental
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture
import cats.Functor
import FileSystemTest.FileSystemTest

@experimental 
class FileSystemPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import language.deprecated.symbolLiterals
  import language.experimental.saferExceptions
  import unsafeExceptions.canThrowAny
  import org.scalatest.matchers.should.Matchers.*
  import FileSystemPathMessages.*
  import FileSystemPathException.*
  import FileSystemPathTest.*

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
              path => assertThat(FileSystemPath.from[FileSystemTest](path).exists),
              path => assertThat(!FileSystemPath.from[FileSystemTest](path).doesNotExist)
            )
          }
          "or not." in { configMap =>
            val file = "FileSystemPathTest-Exists.txt"

            assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
              path => assertThat(FileSystemPath.from[FileSystemTest](path).doesNotExist),
              path => assertThat(!FileSystemPath.from[FileSystemTest](path).exists)
            )
          }
          "is a file, as long as" - { 
            "it exists, independent of an extension being" - {
              "present" in { configMap =>
                val fileName = "FileSystemPathTest-Exists.txt"

                assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFile),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFile)
                )
              }
              "or not." in { configMap =>
                val fileName = "FileSystemPathTest-Exists"

                assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFile),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFile)
                )
              }
            }
            "if it doesn't exist, it either ends with" - {
              "an extension" in { configMap =>
                val file = "FileSystemPathTest-Exists.txt"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFile),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFile)
                )
              }
              "or, something different than '/' and '\\'" in { configMap =>
                val file = "FileSystemPathTest-Exists"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFile),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFile)
                )
              }
            }
          }
          "or, a folder, as long as" - {
            "it exists, independent of being finished in" - { 
              "'/' or '\\'" in { configMap =>
                val folderName = "folder/"

                assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFolder),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFolder)
                )
              }
              "or not." in { configMap =>
                val folderName = "folder"

                assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                  path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFolder),
                  path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFolder)
                )
              }
            }
            "if it doesn't exist, it ends with a '/' or '\\'" in { configMap =>
              val folder = "folder/"

              assertWithNonExisting(folder)(configMap.getRequired("targetDir"))( 
                path => assertThat(FileSystemPath.from[FileSystemTest](path).isAFolder),
                path => assertThat(!FileSystemPath.from[FileSystemTest](path).isNotAFolder)
              )
            }
          }
        }
        /*
         TODO Still not sure if actually creating the resources would be something good to have as
          part of 'FileSystemPath'. I should have more context to decide when and if the opportunity
          arises. I'll evaluate it carefully by then.
         */
        "create the underlying resource it represents, be it a" - { 
          "file" ignore { configMap => }
          "or, a folder." ignore { configMap => }
        }
        "report any failures in creating the underlying resource, be it a" - { 
          "file" ignore { configMap => }
          "or, a folder." ignore { configMap => }
        }
      }
    }
  }

object FileSystemPathTest:
  import java.nio.file.Path
  import org.scalatest.Assertions.assert
  import org.scalatest.compatible.Assertion
  import cats.syntax.functor.*
  import FileSystemTest.emptyState

  private def assertThat[F[_]: Functor](fileSystemQuery: F[Boolean]): F[Assertion] = 
    fileSystemQuery.map(assert(_))

  private def assertWithExistingFile(fileName: String)(buildTarget: String)(assertions: String => FileSystemTest[Assertion]*): Unit = 
    assertWithExisting(fileName, buildTarget, FileSystem[FileSystemTest].createFile(_), assertions: _*)

  private def assertWithExistingFolder(folderName: String)(buildTarget: String)(assertions: String => FileSystemTest[Assertion]*): Unit = 
    assertWithExisting(folderName, buildTarget, FileSystem[FileSystemTest].createFolder(_), assertions: _*)
    
  private def assertWithExisting(
    resourceName: String, 
    buildTarget: String, 
    createResourceAt: Path => FileSystemTest[Unit], 
    assertions: String => FileSystemTest[Assertion]*
  ): Unit =
    val fileSystem = FileSystem[FileSystemTest] 
    val currentFolder = Path.of(s"$buildTarget/test-files")
    val path = Path.of(s"$currentFolder/$resourceName")

    val createResources = for {
      _ <- fileSystem.createFolder(currentFolder)
      _ <- createResourceAt(path)
      _ <- FileSystemTest(fss => (fss, assertions.foreach(_(path.toString).run(fss).value)))
      _ <- fileSystem.delete(path)
      _ <- fileSystem.delete(currentFolder)
    } yield()

    createResources.run(emptyState).value
    
  private def assertWithNonExisting(resourceName: String)(buildTarget: String)(assertions: String => FileSystemTest[Assertion]*): Unit = 
    val currentFolder = s"$buildTarget/test-files"
    val path = s"$currentFolder/$resourceName"

    assertions.foreach(_(path).run(emptyState).value)

  extension(fileSystemTest: FileSystemTest[Boolean])
    private def unary_! : FileSystemTest[Boolean] = fileSystemTest.map(!_)

object FileSystemTest:
  import java.nio.file.Path
  import cats.data.State
  
  enum ResourceType:
    case File, Folder
    
  type FileSystemState = Map[Path, ResourceType]
  type FileSystemTest[A] = State[FileSystemState, A]

  val emptyState: FileSystemState = Map()
  
  def apply[A](s: FileSystemState => (FileSystemState, A)) = State(s)

  import ResourceType.*
  //noinspection NonAsciiCharacters
  // TODO Add law checking to this instance
  given FileSystem[FileSystemTest] with
    def createFile(path: Path): FileSystemTest[Unit] = FileSystemTest(fss ⇒ (fss + (path → File), ()))
    def createFolder(path: Path): FileSystemTest[Unit] = FileSystemTest(fss ⇒ (fss + (path → Folder), ()))
    def delete(path: Path, force: Boolean = false): FileSystemTest[Unit] = FileSystemTest(fss ⇒ (fss - path, ()))
    def exists(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.contains(path)))
    def isAFile(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(File)))
    def isAFolder(path: Path): FileSystemTest[Boolean] = FileSystemTest(fss ⇒ (fss, fss.get(path).contains(Folder)))