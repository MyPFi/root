package com.andreidiego.mpfi.stocks.adapter.files

import java.nio.file.Path
import scala.annotation.experimental
import unsafeExceptions.canThrowAny
import org.scalatest.TryValues.*
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture
import org.scalatest.matchers.should.Matchers.*
import FileSystemTest.{emptyState, FileSystemState, StateFileSystem}

@experimental 
class FileSystemPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import java.io.IOException
  import java.nio.file.DirectoryNotEmptyException
  import language.deprecated.symbolLiterals
  import scala.util.Success
  import scala.collection.immutable.LazyList
  import FileSystemTest.{FileSystemUOE, FileSystemIOE, FileSystemSE}
  import FileSystemPathMessages.*
  import FileSystemPathException.*
  import FileSystemPathTest.*

  "A 'FileSystemPath' should" - {
    "be built from a string representing a well-formed file system path." in { _ =>
      val fileSystemPath = os.home.toString

      "FileSystemPath.from(fileSystemPath)" should compile
    }
    "fail to be built when given" - {
      "an empty string." in { _ =>
        the [RequiredValueMissingException] thrownBy FileSystemPath.from[StateFileSystem]("") should have {
          'message (fileSystemPathMissing)
        }
      }
      "a relative file system path." in { _ =>
        val relativePath = "folder/file.ext"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from[StateFileSystem](relativePath) should have {
          'message (relativeFileSystemPathNotAllowed(relativePath))
        }
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files in Linux (although I couldn't find an authoritative
       source for what is acceptable and what is not)
      */
      "an ill-formed file system path." in { _ =>
        val illFormedPath = s"${os.home}/?"

        the [UnexpectedContentValueException] thrownBy FileSystemPath.from[StateFileSystem](illFormedPath) should have {
          'message (invalidFileSystemPath(illFormedPath))
        }
      }
    }
    "when given a non-empty, well-formed, absolute file system path" - {
      "be able to" - {
        "tell if the file/folder it represents" - {
          "exists" in { configMap =>
            val file = "FileSystemPathTest-Exists.txt"

            assertForExisting(file)(
              basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").exists,
              basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").doesNotExist
            )(configMap.getRequired("targetDir"))
          }
          "or not." in { configMap =>
            val file = "FileSystemPathTest-Exists.txt"

            assertForNonExistingResource(
              basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").doesNotExist,
              basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").exists
            )(configMap.getRequired("targetDir"))
          }
          "is a file, as long as" - {
            "it exists as a file, independent of" - {
              "an extension being" - {
                "present" in { configMap =>
                  val file = "FileSystemPathTest-Exists.txt"

                  assertForExisting(file)(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFolder,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFolder
                  )(configMap.getRequired("targetDir"))
                }
                "or not" in { configMap =>
                  val file = "FileSystemPathTest-Exists"

                  assertForExisting(file)(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFolder,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFolder
                  )(configMap.getRequired("targetDir"))
                }
              }
              "or, even, of it ending with" - {
                "'/'" in { configMap =>
                  val file = "FileSystemPathTest-Exists/"

                  assertForExisting(file.dropRight(1))(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFolder,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFolder
                  )(configMap.getRequired("targetDir"))
                }
                "or '\\'." in { configMap =>
                  val file = "FileSystemPathTest-Exists\\"

                  assertForExisting(file.dropRight(1))(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFolder,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFolder
                  )(configMap.getRequired("targetDir"))
                }
              }
            }
            /* TODO Maybe we should throw an exception if the resource does not exist since Linux is very generous
             *  when it comes to files/folder names and the conventions below may end up causing confusion
             */
            "if it doesn't exist, it either ends with" - {
              "an extension" in { configMap =>
                val file = "FileSystemPathTest-Exists.txt"

                assertForNonExistingResource(
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile
                )(configMap.getRequired("targetDir"))
              }
              "or, something different than '/' and '\\'" in { configMap =>
                val file = "FileSystemPathTest-Exists"

                assertForNonExistingResource(
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$file").isAFile,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$file").isNotAFile
                )(configMap.getRequired("targetDir"))
              }
            }
          }
          "or, a folder, as long as" - {
            "it exists as a folder, independent of" - {
              "being finished in" - {
                "'/' or '\\'" in { configMap =>
                  val folder = "brokerageNotes/"

                  assertForExisting(folder)(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFolder,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFolder,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFile,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFile,
                  )(configMap.getRequired("targetDir"))
                }
                "or not" in { configMap =>
                  val folder = "brokerageNotes"

                  assertForExisting(s"$folder/")(
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFolder,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFolder,
                    basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFile,
                    basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFile,
                  )(configMap.getRequired("targetDir"))
                }
              }
              "or, even, having an extension" in { configMap =>
                val folder = "brokerageNotes.hid"

                assertForExisting(s"$folder/")(
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFolder,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFolder,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFile,
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFile,
                )(configMap.getRequired("targetDir"))
              }
            }
            /* TODO Maybe we should throw an exception if the resource does not exist since Linux is very generous
             *  when it comes to files/folder names and the convention below may end up causing confusion
             */
            "if it doesn't exist, it ends with a" - {
              "'/'" in { configMap =>
                val folder = "folder/"

                assertForNonExistingResource(
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFolder,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFolder
                )(configMap.getRequired("targetDir"))
              }
              "or '\\'." in { configMap =>
                val folder = "folder\\"

                assertForNonExistingResource(
                  basePath => FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isAFolder,
                  basePath => !FileSystemPath.from[StateFileSystem](s"$basePath/$folder").isNotAFolder
                )(configMap.getRequired("targetDir"))
              }
            }
          }
        }
        "create the underlying resource it represents, when the resource does not yet exist, be it a" - {
          "file" in { configMap =>
            val file = "FileSystemPathTest.txt"

            {
              for {
                _ <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                _ <- assertResourceDeleted(file)(configMap.getRequired("targetDir"))
              } yield ()
            }.run(emptyState).value
        }
          "or, a folder." in { configMap =>
            val folder = "folder/"

            {
              for {
                _ <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                _ <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
              } yield ()
            }.run(emptyState).value
          }
        }
        "silent return, when asked to create the underlying resource it represents, if the resource already exists, be it a" - {
          "file" in { configMap =>
            val file = "FileSystemPathTest.txt"
            val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$file")

            val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
            {
              for {
                _         <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                resource  <- fileSystemPath.create
                _          = resource.success.value should be(filePath)
                _         <- assertResourceDeleted(file)(configMap.getRequired("targetDir"))
              } yield ()
            }.run(emptyState).value
          }
          "or, a folder." in { configMap =>
            val folder = "folder/"
            val folderPath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$folder")

            val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$folderPath/")
            {
              for {
                _         <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                resource  <- fileSystemPath.create
                _          = resource.success.value should be(folderPath)
                _         <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
              } yield ()
            }.run(emptyState).value
          }
        }
        "report any failures in creating the underlying resource, be it" - {
          "an 'UnsupportedOperationException' when creating a" - {
            "file" in { configMap =>
              val filePath = s"${configMap.getRequired[String]("targetDir")}/test-files/FileSystemPathTest.txt"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](filePath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemUOE)
                  _         = resource.failure.exception shouldBe a[UnsupportedOperationException]
                } yield ()
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/folder/"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemUOE)
                  _         = resource.failure.exception shouldBe a[UnsupportedOperationException]
                } yield ()
              }.run(emptyState).value
            }
          }
          "an 'IOException' when creating a" - {
            "file" in { configMap =>
              val filePath = s"${configMap.getRequired[String]("targetDir")}/test-files/FileSystemPathTest.txt"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](filePath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemIOE)
                  _         = resource.failure.exception shouldBe a[IOException]
                } yield ()
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/folder/"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemIOE)
                  _         = resource.failure.exception shouldBe a[IOException]
                } yield ()
              }.run(emptyState).value
            }
          }
          "a 'SecurityException' when creating a" - {
            "file" in { configMap =>
              val filePath = s"${configMap.getRequired[String]("targetDir")}/test-files/FileSystemPathTest.txt"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](filePath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemSE)
                  _         = resource.failure.exception shouldBe a[SecurityException]
                } yield ()
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/folder/"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
              {
                for {
                  resource <- fileSystemPath.create(using FileSystemSE)
                  _         = resource.failure.exception shouldBe a[SecurityException]
                } yield ()
              }.run(emptyState).value
            }
          }
          "or, a ResourceWithConflictingTypeAlreadyExistsException when trying to create a" - {
            "file and a folder with that name already exists." in { configMap =>
              val file = "folder"
              val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$file")
              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
              {
                for {
                  _         <- assertResourceCreated(s"$file/", _.isAFolder)(configMap.getRequired("targetDir"))
                  resource  <- fileSystemPath.create
                  exception  = resource.failure.exception
                  _          = exception shouldBe a[ResourceWithConflictingTypeAlreadyExistsException]
                  _          = exception should have message s"Cannot create '$filePath' as a 'File' since it already exists as a 'Folder'."
                  _         <- assertResourceDeleted(s"$file/")(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
            "folder and a file with that name already exists." in { configMap =>
              val folder = "file"
              val folderPath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$folder")
              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$folderPath/")
              {
                for {
                  _         <- assertResourceCreated(folder, _.isAFile)(configMap.getRequired("targetDir"))
                  resource  <- fileSystemPath.create
                  exception  = resource.failure.exception
                  _          = exception shouldBe a[ResourceWithConflictingTypeAlreadyExistsException]
                  _          = exception should have message s"Cannot create '$folderPath' as a 'Folder' since it already exists as a 'File'."
                  _         <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
          }
        }
        "delete the underlying resource it represents, if the resource exists, be it a" - {
          "file" in { configMap =>
            val file = "FileSystemPathTest.txt"
            val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$file")

            val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
            {
              for {
                _             <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                resource      <- fileSystemPath.delete(false)
                _              = resource.success.value should be(filePath)
                doesNotExist  <- fileSystemPath.doesNotExist
                _              = assert(doesNotExist)
              } yield ()
            }.run(emptyState).value
          }
          "or, a folder" - {
            "if empty" in { configMap =>
              val folder = "folder/"
              val folderPath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$folder")

              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$folderPath/")
              {
                for {
                  _             <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                  resource      <- fileSystemPath.delete(false)
                  _              = resource.success.value should be(folderPath)
                  doesNotExist  <- fileSystemPath.doesNotExist
                  _              = assert(doesNotExist)
                } yield ()
              }.run(emptyState).value
            }
            "or, if not empty, when 'force' is set to true." in { configMap =>
              val folder = "folder/"
              val folderPath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$folder")

              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$folderPath/")
              {
                for {
                  _             <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                  _             <- assertResourceCreated(s"$folder/file", _.isAFile)(configMap.getRequired("targetDir"))
                  resource      <- fileSystemPath.delete(true)
                  _              = resource.success.value should be(folderPath)
                  doesNotExist  <- fileSystemPath.doesNotExist
                  _              = assert(doesNotExist)
                } yield ()
              }.run(emptyState).value
            }
          }
        }
        "silent return, when asked to delete the underlying resource it represents and the resource does not exist, be it a" - {
          "file" in { configMap =>
            val file = "FileSystemPathTest.txt"
            val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$file")

            val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
            {
              for {
                doesNotExist  <- fileSystemPath.doesNotExist
                _              = assert(doesNotExist)
                resource      <- fileSystemPath.delete(false)
                _              = resource.success.value should be(filePath)
              } yield ()
            }.run(emptyState).value
          }
          "or, a folder." in { configMap =>
            val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/folder/"

            val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
            {
              for {
                doesNotExist  <- fileSystemPath.doesNotExist
                _              = assert(doesNotExist)
                resource      <- fileSystemPath.delete(false)
                _              = resource.success.value should be(Path.of(folderPath))
              } yield ()
            }.run(emptyState).value
          }
        }
        "report any failures in deleting the underlying resource, be it" - {
          "an 'IOException' when deleting a" - {
            "file" in { configMap =>
              val file = "FileSystemPathTest.txt"
              val filePath = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](filePath)
              {
                for {
                  _        <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                  resource <- fileSystemPath.delete(false)(using FileSystemIOE)
                  _         = resource.failure.exception shouldBe a[IOException]
                  _        <- assertResourceDeleted(file)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folder = "folder/"
              val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
              {
                for {
                  _        <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                  resource <- fileSystemPath.delete(false)(using FileSystemIOE)
                  _         = resource.failure.exception shouldBe a[IOException]
                  _        <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
          }
          "a 'SecurityException' when deleting a" - {
            "file" in { configMap =>
              val file = "FileSystemPathTest.txt"
              val filePath = s"${configMap.getRequired[String]("targetDir")}/test-files/$file"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](filePath)
              {
                for {
                  _        <- assertResourceCreated(file, _.isAFile)(configMap.getRequired("targetDir"))
                  resource <- fileSystemPath.delete(false)(using FileSystemSE)
                  _         = resource.failure.exception shouldBe a[SecurityException]
                  _        <- assertResourceDeleted(file)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
            "or, a folder." in { configMap =>
              val folder = "folder/"
              val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
              {
                for {
                  _        <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                  resource <- fileSystemPath.delete(false)(using FileSystemSE)
                  _         = resource.failure.exception shouldBe a[SecurityException]
                  _        <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
          }
          "or, a 'DirectoryNotEmptyException', when trying to delete a folder which is not empty if the 'force' parameter is set to 'false' (default)." in { configMap =>
            val folder = "folder/"
            val folderPath = s"${configMap.getRequired[String]("targetDir")}/test-files/$folder"

            val fileSystemPath = FileSystemPath.from[StateFileSystem](folderPath)
            {
              for {
                _        <- assertResourceCreated(folder, _.isAFolder)(configMap.getRequired("targetDir"))
                _        <- assertResourceCreated(s"$folder/file", _.isAFile)(configMap.getRequired("targetDir"))
                resource <- fileSystemPath.delete(false)
                _         = resource.failure.exception shouldBe a[DirectoryNotEmptyException]
                exists   <- fileSystemPath.exists
                _         = assert(exists)
                _        <- assertResourceDeleted(folder)(configMap.getRequired("targetDir"))
              } yield ()
            }.run(emptyState).value
          }
        }
        "stream the content of the underlying resource it represents" - {
          "be it a" - {
            "folder (in which case the full paths of its child resources, in alphabetical order, will be streamed)" in { configMap =>
              val brokerageNote1 = "brokerageNote1"
              val brokerageNote2 = "brokerageNote2"
              val brokerageNote3 = "brokerageNote3"
              val paths = Set(brokerageNote2, brokerageNote3, brokerageNote1)

              assertForExisting(paths) { basePath =>
                (
                  FileSystemPath.from[StateFileSystem](basePath).contents,
                  Success(LazyList(
                    s"$basePath${sep}brokerageNote1",
                    s"$basePath${sep}brokerageNote2",
                    s"$basePath${sep}brokerageNote3"
                  )),
                  (actual, expected) ⇒ actual.success.value should contain theSameElementsInOrderAs expected.success.value
                )
              }(configMap.getRequired("targetDir"))
            }
            "or, a file (in which case all lines will be streamed in the original order)" in { configMap ⇒
              val fileName = "brokerageNote.txt"
              val line1 = "aText with"
              val line2 = "with multiple"
              val line3 = "multiple lines"
              val line4 = "lines."
              val content = s"$line1\n$line2\n$line3\n$line4"

              assertForExistingNonEmptyFile(fileName, content) { basePath =>
                (
                  FileSystemPath.from[StateFileSystem](s"$basePath/$fileName").contents,
                  Success(LazyList(line1, line2, line3, line4)),
                  (actual, expected) ⇒ actual.success.value should contain theSameElementsInOrderAs expected.success.value
                )
              }(configMap.getRequired("targetDir"))
            }
          }
          "returning an empty stream, when the underlying resource is empty, be it a" - {
            "folder" in { configMap ⇒
              val folderPath = s"brokerageNotes/"
              val path = Set(folderPath)

              assertForExisting(path) { basePath =>
                (
                  FileSystemPath.from[StateFileSystem](s"$basePath/$folderPath").contents,
                  Success(LazyList.empty),
                  (actual, expected) ⇒ actual.success.value should contain theSameElementsInOrderAs expected.success.value
                )
              }(configMap.getRequired("targetDir"))
            }
            "or, a file" in { configMap ⇒
              val filePath = "brokerageNote1.txt"
              val path = Set(filePath)

              assertForExisting(path) { basePath =>
                (
                  FileSystemPath.from[StateFileSystem](s"$basePath/$filePath").contents,
                  Success(LazyList.empty),
                  (actual, expected) ⇒ actual.success.value should contain theSameElementsInOrderAs expected.success.value
                )
              }(configMap.getRequired("targetDir"))
            }
          }
          "and, reporting a failure when the underlying resource it represents does not exist." in { configMap ⇒
            import FileSystemTest.ResourceType.*
            val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/brokerageNote1.txt")

            val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
            {
              for {
                resource <- fileSystemPath.contents
                isAFile  <- fileSystemPath.isAFile
                _         = if isAFile then File else Folder
                exception = resource.failure.exception
                _         = exception shouldBe a[FileSystem.Exception.ResourceNotFound]
                _         = exception should have message s"Resource '$filePath' cannot be found."
              } yield ()
            }.run(emptyState).value
          }
        }
        "overwrite the contents of a file with a given text" - {
          "when it already exists" in { configMap ⇒
            val filePath = "brokerageNote1.txt"
            val path = Set(filePath)
            val aText = "aText"

            assertForExisting(path) { basePath =>
              val fullFilePath = Path.of(s"$basePath/$filePath")
              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$fullFilePath")
              (
                for {
                  overwritten <- fileSystemPath.overwriteWith(aText)
                  contents    <- fileSystemPath.contents
                } yield (overwritten, contents),
                (Success(fullFilePath), Success(LazyList(aText))),
                (actual, expected) ⇒ {
                  actual._1 should be(expected._1)
                  actual._2.success.value should contain theSameElementsInOrderAs expected._2.success.value
                }
              )
            }(configMap.getRequired("targetDir"))
          }
          "or, create the file containing the given text, if it doesn't exist" in { configMap ⇒
            val filePath = "brokerageNote1.txt"
            val aText = "aText"

            assertForNonExisting { basePath =>
              val fullFilePath = Path.of(s"$basePath/$filePath")
              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$fullFilePath")
              (
                for {
                  overwritten <- fileSystemPath.overwriteWith(aText)
                  contents <- fileSystemPath.contents
                  deleted <- fileSystemPath.delete(true)
                } yield (overwritten, contents, deleted),
                (Success(fullFilePath), Success(LazyList(aText)), Success(fullFilePath)),
                (actual, expected) ⇒ {
                  actual._1 should be(expected._1)
                  actual._2.success.value should contain theSameElementsInOrderAs expected._2.success.value
                  actual._3 should be(expected._3)
                }
              )
            }(configMap.getRequired("targetDir"))
          }
          "reporting a failure in case" - {
            "the underlying resource it represents is a folder" in { configMap ⇒
              val folderName = "brokerageNotes/"
              val folderPath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$folderName")
              val aText = "aText"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$folderPath/")
              {
                for {
                  _         <- assertResourceCreated(folderName, _.isAFolder)(configMap.getRequired("targetDir"))
                  resource  <- fileSystemPath.overwriteWith(aText)
                  exception  = resource.failure.exception
                  _          = exception shouldBe a[ResourceWithConflictingTypeAlreadyExistsException]
                  _          = exception should have message s"Cannot create '$folderPath' as a 'File' since it already exists as a 'Folder'."
                  _         <- assertResourceDeleted(folderName)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
            "or, the underlying 'FileSystem' fails when trying to delete the existing file." in { configMap ⇒
              val fileName = "brokerageNote1.txt"
              val filePath = Path.of(s"${configMap.getRequired[String]("targetDir")}/test-files/$fileName")
              val aText = "aText"

              val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$filePath")
              {
                for {
                  _         <- assertResourceCreated(fileName, _.isAFile)(configMap.getRequired("targetDir"))
                  resource  <- fileSystemPath.overwriteWith(aText)(using FileSystemIOE)
                  exception  = resource.failure.exception
                  _          = exception shouldBe a[IOException]
                  _          = exception should have message "IOException"
                  _         <- assertResourceDeleted(fileName)(configMap.getRequired("targetDir"))
                } yield ()
              }.run(emptyState).value
            }
          }
        }
      }
    }
  }

object FileSystemPathTest:
  import org.scalatest.Assertions.assert
  import org.scalatest.Assertion

  private type FileSystemPathQuery = FileSystemPath[StateFileSystem] => StateFileSystem[Boolean]
  private val sep: Char = java.io.File.separatorChar

  private def assertForExisting(resourceName: String)
                               (assertions: String => StateFileSystem[Boolean]*)
                               (buildTarget: String)
  : Unit =
    assertForExisting(Set(resourceName))(assertions.map(assertion ⇒ (fullPath: String) ⇒ (
      assertion(fullPath),
      true,
      (actual: Boolean, expected: Boolean) ⇒ actual should equal(expected)
    )): _*)(buildTarget)

  private def assertForNonExistingResource(assertions: String => StateFileSystem[Boolean]*)
                                          (buildTarget: String)
  : Unit =
    assertForNonExisting(assertions.map(assertion ⇒ (fullPath: String) ⇒ (
      assertion(fullPath),
      true,
      (actual: Boolean, expected: Boolean) ⇒ actual should equal(expected)
    )): _*)(buildTarget)

  private def assertResourceCreated(resourceName: String, resourceType: FileSystemPathQuery)
                                   (buildTarget: String)
  : StateFileSystem[Unit] =
    import cats.Functor
    import cats.syntax.functor.*

    def assertThat[F[_]: Functor](fileSystemQuery: F[Boolean]): F[Assertion] =
      fileSystemQuery.map(assert(_))

    val pathString = s"$buildTarget/test-files/$resourceName"
    val fileSystemPath = FileSystemPath.from[StateFileSystem](pathString)

    for {
      _ <- assertThat(fileSystemPath.doesNotExist)
      _ <- fileSystemPath.create.map(_.success.value should be (Path.of(pathString)))
      _ <- assertThat(fileSystemPath.exists)
      _ <- assertThat(resourceType(fileSystemPath))
    } yield ()

  private def assertResourceDeleted(resourceName: String)
                                   (buildTarget: String)
  : StateFileSystem[Unit] =
    import cats.Functor
    import cats.syntax.functor.*

    def assertThat[F[_]: Functor](fileSystemQuery: F[Boolean]): F[Assertion] =
      fileSystemQuery.map(assert(_))

    val pathString = s"$buildTarget/test-files/$resourceName"
    val fileSystemPath = FileSystemPath.from[StateFileSystem](pathString)

    for {
      _ <- assertThat(fileSystemPath.exists)
      _ <- fileSystemPath.delete(true).map(_.success.value should be (Path.of(pathString)))
      _ <- assertThat(fileSystemPath.doesNotExist)
    } yield ()

  private def assertForExisting[T](resourceNames: Set[String])
                                  (assertions: String ⇒ (StateFileSystem[T], T, (T, T) ⇒ Assertion)*)
                                  (buildTarget: String)
  : Unit =
    val currentFolder = Path.of(s"$buildTarget/test-files")
    val fileSystemPaths = resourceNames
      .map(path ⇒ s"$currentFolder/$path")
      .map(fullPath ⇒ FileSystemPath.from[StateFileSystem](fullPath))
    val initialState = (fileSystemPaths + FileSystemPath.from[StateFileSystem](s"$currentFolder/"))
      .foldLeft(emptyState) { (state, fileSystemPath) =>
        fileSystemPath.create.runS(state).value
      }

    run(assertions: _*)(buildTarget, initialState)

    fileSystemPaths
      .foldLeft(initialState) { (state, fileSystemPath) =>
        fileSystemPath.delete(true).runS(state).value
      }

  private def assertForExistingNonEmptyFile[T](path: String, content: String)
                                              (assertions: ⇒ String ⇒ (StateFileSystem[T], T, (T, T) ⇒ Assertion)*)
                                              (buildTarget: String)
  : Unit =
    val currentFolder = Path.of(s"$buildTarget/test-files")
    val fileSystemPath = FileSystemPath.from[StateFileSystem](s"$currentFolder/$path")

    val program = for {
      _ <- fileSystemPath.create
      _ <- fileSystemPath.overwriteWith(content)
      _ <- lift(assertions: _*)(buildTarget)
      _ <- fileSystemPath.delete(true)
    } yield ()

    program.run(emptyState).value

  private def assertForNonExisting[T](assertions: String ⇒ (StateFileSystem[T], T, (T, T) ⇒ Assertion)*)
                                     (buildTarget: String)
  : Unit =
    run(assertions: _*)(buildTarget, emptyState)

  private def run[T](assertions: String ⇒ (StateFileSystem[T], T, (T, T) ⇒ Assertion)*)
                    (buildTarget: String, originalState: FileSystemState)
  : (FileSystemState, Seq[Assertion]) =
    lift(assertions: _*)(buildTarget)
      .run(originalState)
      .value

  private def lift[T](assertions: ⇒ String ⇒ (StateFileSystem[T], T, (T, T) ⇒ Assertion)*)
                     (buildTarget: String)
  : StateFileSystem[Seq[Assertion]] =
    val currentFolder = Path.of(s"$buildTarget/test-files")

    FileSystemTest(state => (state, assertions.map(assertion ⇒ {
      val actual: T = assertion(s"$currentFolder")._1
        .runA(state)
        .value
      val expected: T = assertion(s"$currentFolder")._2

      assertion(s"$currentFolder")._3(actual, expected)
    })))

  extension(fileSystemTest: StateFileSystem[Boolean])
    private def unary_! : StateFileSystem[Boolean] = fileSystemTest.map(!_)