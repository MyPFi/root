package com.andreidiego.mpfi.stocks.adapter.files

import scala.annotation.experimental
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture

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
    "be built from a string representing a well-formed file system path." in { configMap =>
      val fileSystemPath: String = os.home.toString

      "FileSystemPath.from(fileSystemPath)" should compile
    }
    "fail to be built when given" - {
      "an empty string." in { configMap =>
        the [RequiredValueMissingException] thrownBy FileSystemPath.from("") should have {
          'message (fileSystemPathMissing)
        }
      }
      "a relative file system path." in { configMap =>
        val relativePath = "folder/file.ext"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from(relativePath) should have {
          'message (relativeFileSystemPathNotAllowed(relativePath))
        }
      }
      /* 
      TODO This test will probably fail if run on Linux since it looks like almost everything is possible when it comes 
      to naming files in Linux  - although I couldn't find an authoritative source for what is acceptable and what is not.
      */
      "a ill-formed file system path." in { configMap =>
        val illFormedPath = s"${os.home}/?"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from(illFormedPath) should have {
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
              path => assert(FileSystemPath.from(path).exists),
              path => assert(!FileSystemPath.from(path).doesNotExist)
            )
          }
          "or not." in { configMap =>
            val file = "FileSystemPathTest-Exists.txt"

            assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
              path => assert(FileSystemPath.from(path).doesNotExist),
              path => assert(!FileSystemPath.from(path).exists)
            )
          }
          "is a file, as long as" - { 
            "it exists, independent of an extension being" - {
              "present" in { configMap =>
                val fileName = "FileSystemPathTest-Exists.txt"

                assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFile),
                  path => assert(!FileSystemPath.from(path).isNotAFile)
                )
              }
              "or not." in { configMap =>
                val fileName = "FileSystemPathTest-Exists"

                assertWithExistingFile(fileName)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFile),
                  path => assert(!FileSystemPath.from(path).isNotAFile)
                )
              }
            }
            "if it doesn't exist, it either ends with" - {
              "an extension" in { configMap =>
                val file = "FileSystemPathTest-Exists.txt"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFile),
                  path => assert(!FileSystemPath.from(path).isNotAFile)
                )
              }
              "or, something different than '/' and '\\'" in { configMap =>
                val file = "FileSystemPathTest-Exists"

                assertWithNonExisting(file)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFile),
                  path => assert(!FileSystemPath.from(path).isNotAFile)
                )
              }
            }
          }
          "or, a folder, as long as" - {
            "it exists, independent of being finished in" - { 
              "'/' or '\\'" in { configMap =>
                val folderName = "folder/"

                assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFolder),
                  path => assert(!FileSystemPath.from(path).isNotAFolder)
                )
              }
              "or not." in { configMap =>
                val folderName = "folder"

                assertWithExistingFolder(folderName)(configMap.getRequired("targetDir"))( 
                  path => assert(FileSystemPath.from(path).isAFolder),
                  path => assert(!FileSystemPath.from(path).isNotAFolder)
                )
              }
            }
            "if it doesn't exist, it ends with a '/' or '\\'" in { configMap =>
              val folder = "folder/"

              assertWithNonExisting(folder)(configMap.getRequired("targetDir"))( 
                path => assert(FileSystemPath.from(path).isAFolder),
                path => assert(!FileSystemPath.from(path).isNotAFolder)
              )
            }
          }
        }
        // TODO Still not sure if actually creating the resources would be something good to have as part of 'FileSystemPath'. 
        // I should have more context to decide when and if the opportunity arises. I'll evaluate it carefully by then.
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
  import java.nio.file.Files
  import org.scalatest.compatible.Assertion

  private def assertWithExistingFile(fileName: String)(buildTarget: String)(assertions: String => Assertion*): Unit = 
    assertWithExisting(fileName, buildTarget, Files.createFile(_), assertions: _*)

  private def assertWithExistingFolder(folderName: String)(buildTarget: String)(assertions: String => Assertion*): Unit = 
    assertWithExisting(folderName, buildTarget, Files.createDirectory(_), assertions: _*)
    
  private def assertWithExisting(resourceName: String, buildTarget: String, createResourceAt: Path => Unit, assertions: String => Assertion*): Unit = 
    val currentFolder = Path.of(s"$buildTarget/test-files")
    val path = Path.of(s"$currentFolder/$resourceName")
    
    
    Files.createDirectory(currentFolder)
    createResourceAt(path)
    
    assertions.foreach(_(path.toString))

    Files.delete(path)
    Files.delete(currentFolder)

  private def assertWithNonExisting(resourceName: String)(buildTarget: String)(assertions: String => Assertion*): Unit = 
    val currentFolder = s"$buildTarget/test-files"
    val path = s"$currentFolder/$resourceName"

    assertions.foreach(_(path))