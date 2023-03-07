package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import scala.annotation.experimental
import org.scalatest.fixture.ConfigMapFixture
import org.scalatest.freespec.FixtureAnyFreeSpec
import com.andreidiego.mpfi.stocks.adapter.files
import files.FileSystemTest.StateFileSystem

@experimental class ExtractionGuideSpecsPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import java.nio.file.Path
  import language.deprecated.symbolLiterals
  import org.scalatest.matchers.should.Matchers.*
  import files.FileSystem
  import files.FileSystemMessages.resourceNotFound
  import files.FileSystemPath.Exceptions.*
  import files.FileSystemPath.Messages.*
  import ExtractionGuideSpecsPath.Messages.*
  import ExtractionGuideSpecsPath.Sorters.*
  import ExtractionGuideSpecsPathTest.*

  "A 'ExtractionGuideSpecsPathTest' should" - {
    "be built from a string representing the absolute path to a folder that is supposed to contain extraction guide spec files in TXT format." in { _ =>
      val extractionGuideSpecsFolder = s"${os.home}/extractionGuideSpecs"

      "ExtractionGuideSpecsPath.from[StateFileSystem](extractionGuideSpecsFolder)" should compile
    }
    "fail to be built when given" - {
      "an empty string," in { _ =>
        ExtractionGuideSpecsPath.from[StateFileSystem]("").exception should have(
          'class(classOf[RequiredValueMissing]),
          'message(fileSystemPathMissing)
        )
      }
      "a relative file system path," in { _ =>
        val relativePath = "folder/"

        ExtractionGuideSpecsPath.from[StateFileSystem](relativePath).exception should have(
          'class(classOf[UnexpectedContentValue]),
          'message(relativeFileSystemPathNotAllowed(relativePath))
        )
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files/folders in Linux (although I couldn't find an authoritative
       source for what is acceptable and what is not).
      */
      "an ill-formed absolute file system path," in { _ =>
        val illFormedPath = s"${os.home}/?"

        ExtractionGuideSpecsPath.from[StateFileSystem](illFormedPath).exception should have(
          'class(classOf[UnexpectedContentValue]),
          'message(invalidFileSystemPath(illFormedPath))
        )
      }
      "or, a well-formed absolute file system path to either a" - {
        "file instead of a folder," in { configMap =>
          val file = "file.ext"

          assertForExisting(file) { (fullPath, fss) ⇒
            ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).exception(fss) should have(
              'class(classOf[UnexpectedContentValue]),
              'message(filePathNotAllowed(fullPath))
            )
          }(configMap.getRequired("targetDir"))
        }
        "or, to a folder that" - {
          "doesn't exist," in { _ =>
            val folderPath = s"${os.home}/extractionGuideSpecsFolder/"

            ExtractionGuideSpecsPath.from[StateFileSystem](folderPath).exception should have(
              'class(classOf[FileSystem.Exception.ResourceNotFound]),
              'message(resourceNotFound(folderPath))
            )
          }
          "is empty," in { configMap =>
            val folder = "emptyFolder/"

            assertForExisting(folder) { (fullPath, fss) ⇒
              ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).exception(fss) should have(
                'class(classOf[UnexpectedContentValue]),
                'message(emptyFolderNotAllowed(fullPath))
              )
            }(configMap.getRequired("targetDir"))
          }
          "or, while non-empty, does not contain at least one ExtractionGuideSpecPath (e.g. 'nota_corretagem-modal_mais-v1.txt')." in { configMap =>
            val folder = "extractionGuideSpecsFolder/"
            val extractionGuideSpecs = Seq(
              "nota_corretagem",
              "nota_corretagem-modal_mais.txt",
              "nota_corretagem-modal_mais-v1"
            )

            assertForExisting(folder, extractionGuideSpecs) { (fullPath, fss) ⇒
              ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).exception(fss) should have(
                'class(classOf[UnexpectedContentValue]),
                'message(noExtractionGuideSpecPathFoundIn(fullPath))
              )
            }(configMap.getRequired("targetDir"))
          }
        }
      }
    }
    "when given a non-empty, well-formed, absolute file system path to a folder containing at least one ExtractionGuideSpecPath, be able to provide a 'FileSystemPath' instance that tells that the resource it represents" - {
      "is a folder," in { configMap =>
        val folder = "extractionGuideSpecsFolder/"
        val extractionGuideSpecs = Seq("nota_corretagem-modal_mais-v1.txt")

        assertBooleanForExisting(folder, extractionGuideSpecs) (
          (fullPath, fss) => ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).isAFolder(fss),
          (fullPath, fss) => !ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).isNotAFolder(fss)
        )(configMap.getRequired("targetDir"))
      }
      "not a file." in { configMap =>
        val folder = "extractionGuideSpecsFolder/"
        val extractionGuideSpecs = Seq("nota_corretagem-modal_mais-v1.txt")

        assertBooleanForExisting(folder, extractionGuideSpecs)(
          (fullPath, fss) => ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).isNotAFile(fss),
          (fullPath, fss) => !ExtractionGuideSpecsPath.from[StateFileSystem](fullPath).isAFile(fss)
        )(configMap.getRequired("targetDir"))
      }
    }
    "provide a set of 'Sorter' implementations that would allow the children 'FileSystemPath's of a 'ExtractionGuideSpecsPath' to be ordered by their" - {
      "full filenames," in { configMap =>
        val folder = "extractionGuideSpecsFolder/"
        val extractionGuideSpec1 = "broker3/nota_corretagem-agora-v1.txt"
        val extractionGuideSpec2 = "broker1/nota_corretagem-agora-v2.txt"
        val extractionGuideSpec3 = "broker2/nota_corretagem-modal_mais-v1.txt"
        val extractionGuideSpecs = Seq(
          extractionGuideSpec3,
          extractionGuideSpec1,
          extractionGuideSpec2
        )

        assertForExisting(folder, extractionGuideSpecs) { (fullPath, fss) ⇒
          ExtractionGuideSpecsPath.from[StateFileSystem](fullPath)
            .contents(fss)(fullFilenameSorter) should contain theSameElementsInOrderAs Seq(
            s"${Path.of(s"$fullPath/$extractionGuideSpec1")}",
            s"${Path.of(s"$fullPath/$extractionGuideSpec2")}",
            s"${Path.of(s"$fullPath/$extractionGuideSpec3")}"
          )
        }(configMap.getRequired("targetDir"))
      }
      "or, their filename components, namely:" - {
        "DocumentType," in { configMap =>
          val folder = "extractionGuideSpecsFolder/"
          val extractionGuideSpec1 = "broker3/extrato-modal_mais-v1.txt"
          val extractionGuideSpec2 = "broker1/fatura-modal_mais-v1.txt"
          val extractionGuideSpec3 = "broker2/nota_corretagem-modal_mais-v1.txt"
          val extractionGuideSpecs = Seq(
            extractionGuideSpec3,
            extractionGuideSpec1,
            extractionGuideSpec2
          )

          assertForExisting(folder, extractionGuideSpecs) { (fullPath, fss) ⇒
            ExtractionGuideSpecsPath.from[StateFileSystem](fullPath)
              .contents(fss)(documentTypeSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$extractionGuideSpec1")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec2")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec3")}"
            )
          }(configMap.getRequired("targetDir"))
        }
        "DocumentIssuer," in { configMap =>
          val folder = "extractionGuideSpecsFolder/"
          val extractionGuideSpec1 = "broker3/nota_corretagem-agora-v1.txt"
          val extractionGuideSpec2 = "broker1/nota_corretagem-modal_mais-v1.txt"
          val extractionGuideSpec3 = "broker2/nota_corretagem-xp-v1.txt"
          val extractionGuideSpecs = Seq(
            extractionGuideSpec3,
            extractionGuideSpec1,
            extractionGuideSpec2
          )

          assertForExisting(folder, extractionGuideSpecs) { (fullPath, fss) ⇒
            ExtractionGuideSpecsPath.from[StateFileSystem](fullPath)
              .contents(fss)(documentIssuerSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$extractionGuideSpec1")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec2")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec3")}"
            )
          }(configMap.getRequired("targetDir"))
        }
        "or, DocumentVersion." in { configMap =>
          val folder = "extractionGuideSpecsFolder/"
          val extractionGuideSpec1 = "broker1/nota_corretagem-agora-v1.txt"
          val extractionGuideSpec2 = "broker1/nota_corretagem-agora-v2.txt"
          val extractionGuideSpec3 = "broker1/nota_corretagem-agora-v10.txt"
          val extractionGuideSpecs = Seq(
            extractionGuideSpec3,
            extractionGuideSpec1,
            extractionGuideSpec2
          )

          assertForExisting(folder, extractionGuideSpecs) { (fullPath, fss) ⇒
            ExtractionGuideSpecsPath.from[StateFileSystem](fullPath)
              .contents(fss)(documentVersionSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$extractionGuideSpec1")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec2")}",
              s"${Path.of(s"$fullPath/$extractionGuideSpec3")}"
            )
          }(configMap.getRequired("targetDir"))
        }
      }
    }
  }

object ExtractionGuideSpecsPathTest:
  import unsafeExceptions.canThrowAny
  import scala.util.Try
  import org.scalatest.compatible.Assertion
  import org.scalatest.Assertions.assert
  import org.scalatest.EitherValues.*
  import org.scalatest.TryValues.*
  import files.FileSystemTest
  import FileSystemTest.{emptyState, FileSystemState}
  import files.FileSystemPath

  private def assertForExisting(resourceName: String, contents: Seq[String] = Seq.empty)
                               (assertions: (String, FileSystemState) ⇒ Assertion*)
                               (buildTarget: String)
  : Unit =
    val fullPath = s"$buildTarget/test-files/$resourceName"
    val fileSystemPath = FileSystemPath.from[StateFileSystem](fullPath)

    val actionsOnMainResource = for {
      _ <- fileSystemPath.create
      _ <- FileSystemTest(fss ⇒ (fss, assertions.map(assertion ⇒ assertion(fullPath, fss))))
      _ <- fileSystemPath.delete(true)
    } yield()

    val childrenResources = contents
      .map(line ⇒ FileSystemPath.from[StateFileSystem](s"$fullPath/$line"))

    val createChildren = childrenResources
      .foldLeft(emptyState) { (state, fileSystemPath) ⇒
        fileSystemPath.create.runS(state).value
      }

    childrenResources
      .foldLeft(actionsOnMainResource.runS(createChildren).value) { (state, fileSystemPath) ⇒
        fileSystemPath.delete(true).runS(state).value
      }

  private def assertBooleanForExisting(resourceName: String, contents: Seq[String] = Seq.empty)
                                      (assertions: (String, FileSystemState) ⇒ Boolean*)
                                      (buildTarget: String)
  : Unit =
    assertForExisting(resourceName, contents)(assertions.map { assertion =>
      (fullPath: String, state: FileSystemState) ⇒ assert(assertion(fullPath, state))
    }: _*)(buildTarget)

  extension (fileSystemTest: StateFileSystem[Try[ExtractionGuideSpecsPath[StateFileSystem]]])

    private def exception: Throwable = exception(emptyState)

    private def exception(state: FileSystemState): Throwable =
      run(fileSystemTest.map(_.failure.exception))(state)

    private def isAFolder(state: FileSystemState = emptyState): Boolean = {
      run(fileSystemPath(state).isAFolder)(state)
    }

    private def isNotAFolder(state: FileSystemState = emptyState): Boolean =
      run(fileSystemPath(state).isNotAFolder)(state)

    private def isNotAFile(state: FileSystemState = emptyState): Boolean =
      run(fileSystemPath(state).isNotAFile)(state)

    private def isAFile(state: FileSystemState = emptyState): Boolean =
      run(fileSystemPath(state).isAFile)(state)

    private def contents(state: FileSystemState): LazyList[String] =
      run(fileSystemPath(state).contents)(state)
        .success
        .value

    private def contents(state: FileSystemState = emptyState)(sortedWith: (String, String) ⇒ Boolean): LazyList[String] =
      run(fileSystemPath(state).contents(sortedWith))(state)
        .success
        .value

    private def run[A, F[A] <: StateFileSystem[A]](f: F[A])(state: FileSystemState): A = f.runA(state).value

    private def fileSystemPath(state: FileSystemState = emptyState): FileSystemPath[StateFileSystem] =
      run(fileSystemTest.map(_.success.value))(state)