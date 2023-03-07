package com.andreidiego.mpfi.stocks.adapter.files.readers.pdf.poc

import java.nio.file.Path
import scala.annotation.experimental
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture
import org.scalatest.matchers.should.Matchers.*
import com.andreidiego.mpfi.stocks.adapter.files
import files.FileSystemTest.StateFileSystem

@experimental class PDFBrokerageNotesPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import language.deprecated.symbolLiterals
  import files.FileSystem
  import files.FileSystemMessages.resourceNotFound
  import files.FileSystemPath.Exceptions.*
  import files.FileSystemPath.Messages.*
  import PDFBrokerageNotesPath.Messages.*
  import PDFBrokerageNotesPath.Sorters.*
  import PDFBrokerageNotesPathTest.*

  "A 'PDFBrokerageNotesPathTest' should" - {
    "be built from a string representing the absolute path to a folder that is supposed to contain brokerage note files in PDF format." in { _ =>
      val brokerageNotesFolder = s"${os.home}/brokerageNotes"

      "PDFBrokerageNotesPath.from[StateFileSystem](brokerageNotesFolder)" should compile
    }
    "fail to be built when given" - {
      "an empty string," in { _ =>
        PDFBrokerageNotesPath.from[StateFileSystem]("").exception should have(
          'class(classOf[RequiredValueMissing]),
          'message(fileSystemPathMissing)
        )
      }
      "a relative file system path," in { _ =>
        val relativePath = "folder/"

        PDFBrokerageNotesPath.from[StateFileSystem](relativePath).exception should have(
          'class(classOf[UnexpectedContentValue]),
          'message(relativeFileSystemPathNotAllowed(relativePath))
        )
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files in Linux (although I couldn't find an authoritative
       source for what is acceptable and what is not).
      */
      "an ill-formed absolute file system path," in { _ =>
        val illFormedPath = s"${os.home}/?"

        PDFBrokerageNotesPath.from[StateFileSystem](illFormedPath).exception should have(
          'class(classOf[UnexpectedContentValue]),
          'message(invalidFileSystemPath(illFormedPath))
        )
      }
      "or, a well-formed absolute file system path to either a" - {
        "file instead of a folder," in { configMap =>
          val file = "file.ext"

          assertForExisting(file) { (fullPath, fss) ⇒
            PDFBrokerageNotesPath.from[StateFileSystem](fullPath).exception(fss) should have(
              'class(classOf[UnexpectedContentValue]),
              'message(filePathNotAllowed(fullPath))
            )
          }(configMap.getRequired("targetDir"))
        }
        "or, to a folder that" - {
          "doesn't exist," in { _ =>
            val folderPath = s"${os.home}/brokerageNotesFolder/"

            PDFBrokerageNotesPath.from[StateFileSystem](folderPath).exception should have(
              'class(classOf[FileSystem.Exception.ResourceNotFound]),
              'message(resourceNotFound(folderPath))
            )
          }
          "is empty," in { configMap =>
            val folder = "emptyFolder/"

            assertForExisting(folder) { (fullPath, fss) ⇒
              PDFBrokerageNotesPath.from[StateFileSystem](fullPath).exception(fss) should have(
                'class(classOf[UnexpectedContentValue]),
                'message(emptyFolderNotAllowed(fullPath))
              )
            }(configMap.getRequired("targetDir"))
          }
          "or, while non-empty, does not contain at least one PDFBrokerageNotePath (e.g. 'vendaVALE3 - 21306 - 08-03-2022.pdf')." in { configMap =>
            val folder = "brokerageNotesFolder/"
            val brokerageNotes = Seq(
              "vendaVALE3",
              "vendaVALE3 - 21306.pdf",
              "vendaVALE3 - 21306 - 08-03-2022"
            )

            assertForExisting(folder, brokerageNotes) { (fullPath, fss) ⇒
              PDFBrokerageNotesPath.from[StateFileSystem](fullPath).exception(fss) should have(
                'class(classOf[UnexpectedContentValue]),
                'message(noPDFBrokerageNotePathFoundIn(fullPath))
              )
            }(configMap.getRequired("targetDir"))
          }
        }
      }
    }
    "when given a non-empty, well-formed, absolute file system path to a folder containing at least one PDFBrokerageNotePath, be able to provide a 'FileSystemPath' instance that tells that the resource it represents" - {
      "is a folder," in { configMap =>
        val folder = "brokerageNotesFolder/"
        val brokerageNotes = Seq("vendaVALE3 - 21306 - 08-03-2022.pdf")

        assertBooleanForExisting(folder, brokerageNotes) (
          (fullPath, fss) => PDFBrokerageNotesPath.from[StateFileSystem](fullPath).isAFolder(fss),
          (fullPath, fss) => !PDFBrokerageNotesPath.from[StateFileSystem](fullPath).isNotAFolder(fss)
        )(configMap.getRequired("targetDir"))
      }
      "not a file." in { configMap =>
        val folder = "brokerageNotesFolder/"
        val brokerageNotes = Seq("vendaVALE3 - 21306 - 08-03-2022.pdf")

        assertBooleanForExisting(folder, brokerageNotes)(
          (fullPath, fss) => PDFBrokerageNotesPath.from[StateFileSystem](fullPath).isNotAFile(fss),
          (fullPath, fss) => !PDFBrokerageNotesPath.from[StateFileSystem](fullPath).isAFile(fss)
        )(configMap.getRequired("targetDir"))
      }
    }
    "provide a set of 'Sorter' implementations that would allow the 'content' of a 'FileSystemPath' representing a folder to be ordered by their" - {
      "full filenames," in { configMap =>
        val folder = "brokerageNotesFolder/"
        val brokerageNote1 = "broker3/compraVALE3 - 21306 - 08-03-2022.pdf"
        val brokerageNote2 = "broker1/vendaVALE3 - 21306 - 08-03-2022.pdf"
        val brokerageNote3 = "broker2/vendaVALE3 - 21307 - 08-03-2022.pdf"
        val brokerageNotes = Seq(
          brokerageNote3,
          brokerageNote1,
          brokerageNote2
        )

        assertForExisting(folder, brokerageNotes) { (fullPath, fss) ⇒
          PDFBrokerageNotesPath.from[StateFileSystem](fullPath)
            .contents(fss)(fullFilenameSorter) should contain theSameElementsInOrderAs Seq(
            s"${Path.of(s"$fullPath/$brokerageNote1")}",
            s"${Path.of(s"$fullPath/$brokerageNote2")}",
            s"${Path.of(s"$fullPath/$brokerageNote3")}"
          )
        }(configMap.getRequired("targetDir"))
      }
      "or, their filename components, namely:" - {
        "OperationsDescription," in { configMap =>
          val folder = "brokerageNotesFolder/"
          val brokerageNote1 = "broker3/compraVALE3 - 21306 - 08-03-2022.pdf"
          val brokerageNote2 = "broker1/vendaVALE3 - 21306 - 08-03-2022.pdf"
          val brokerageNote3 = "broker2/vendaVALE3 - 21307 - 08-03-2022.pdf"
          val brokerageNotes = Seq(
            brokerageNote3,
            brokerageNote1,
            brokerageNote2
          )

          assertForExisting(folder, brokerageNotes) { (fullPath, fss) ⇒
            PDFBrokerageNotesPath.from[StateFileSystem](fullPath)
              .contents(fss)(operationsDescriptionSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$brokerageNote1")}",
              s"${Path.of(s"$fullPath/$brokerageNote3")}",
              s"${Path.of(s"$fullPath/$brokerageNote2")}"
            )
          }(configMap.getRequired("targetDir"))
        }
        "NoteNumber," in { configMap =>
          val folder = "brokerageNotesFolder/"
          val brokerageNote1 = "broker3/compraVALE3 - 21306 - 08-03-2022.pdf"
          val brokerageNote2 = "broker1/vendaVALE3 - 21306 - 08-03-2022.pdf"
          val brokerageNote3 = "broker2/vendaVALE3 - 21307 - 08-03-2022.pdf"
          val brokerageNotes = Seq(
            brokerageNote3,
            brokerageNote1,
            brokerageNote2
          )

          assertForExisting(folder, brokerageNotes) { (fullPath, fss) ⇒
            PDFBrokerageNotesPath.from[StateFileSystem](fullPath)
              .contents(fss)(noteNumberSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$brokerageNote1")}",
              s"${Path.of(s"$fullPath/$brokerageNote2")}",
              s"${Path.of(s"$fullPath/$brokerageNote3")}"
            )
          }(configMap.getRequired("targetDir"))
        }
        "or, TradingDate." in { configMap =>
          val folder = "brokerageNotesFolder/"
          val brokerageNote1 = "broker3/compraVALE3 - 21306 - 07-03-2022.pdf"
          val brokerageNote2 = "broker1/vendaVALE3 - 21306 - 08-03-2022.pdf"
          val brokerageNote3 = "broker2/vendaVALE3 - 21307 - 09-03-2022.pdf"
          val brokerageNotes = Seq(
            brokerageNote3,
            brokerageNote1,
            brokerageNote2
          )

          assertForExisting(folder, brokerageNotes) { (fullPath, fss) ⇒
            PDFBrokerageNotesPath.from[StateFileSystem](fullPath)
              .contents(fss)(tradingDateSorter) should contain theSameElementsInOrderAs Seq(
              s"${Path.of(s"$fullPath/$brokerageNote1")}",
              s"${Path.of(s"$fullPath/$brokerageNote2")}",
              s"${Path.of(s"$fullPath/$brokerageNote3")}"
            )
          }(configMap.getRequired("targetDir"))
        }
      }
    }
  }

object PDFBrokerageNotesPathTest:
  import unsafeExceptions.canThrowAny
  import scala.util.Try
  import org.scalatest.compatible.Assertion
  import org.scalatest.Assertions.assert
  import org.scalatest.TryValues.*
  import org.scalatest.EitherValues.*
  import files.FileSystemTest
  import FileSystemTest.FileSystemState
  import FileSystemTest.emptyState
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

  extension (fileSystemTest: StateFileSystem[Try[PDFBrokerageNotesPath[StateFileSystem]]])

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