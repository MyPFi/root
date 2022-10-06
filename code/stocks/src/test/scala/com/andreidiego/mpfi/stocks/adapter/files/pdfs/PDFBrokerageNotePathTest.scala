package com.andreidiego.mpfi.stocks.adapter.files.pdfs

import scala.annotation.experimental
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture
import com.andreidiego.mpfi.stocks.adapter.files.FileSystemTest.FileSystemTest

@experimental
class PDFBrokerageNotePathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import unsafeExceptions.canThrowAny
  import language.deprecated.symbolLiterals
  import org.scalatest.matchers.should.Matchers.*
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathException.*
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathMessages.*
  import PDFBrokerageNotePathMessages.*
  import PDFBrokerageNotePathTest.*

  "A 'PDFBrokerageNotePath' should" - {
    "be built from a string representing the path to a PDF file." in { _ =>
      val pdfPath: String = s"${os.home.toString}/file.pdf"

      "PDFBrokerageNotePath.from(pdfPath)" should compile
    }
    "fail to be built when given" - {
      "an empty string." in { _ =>
        the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest]("") should have {
          'message(fileSystemPathMissing)
        }
      }
      "a relative file system path." in { _ =>
        val relativePath = "folder/file.pdf"

        the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](relativePath) should have {
          'message(relativeFileSystemPathNotAllowed(relativePath))
        }
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files in Linux  - although I couldn't find an authoritative
       source for what is acceptable and what is not.
      */
      "an ill-formed file system path." in { _ =>
        val illFormedPath = s"${os.home}/?"

        the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](illFormedPath) should have {
          'message(invalidFileSystemPath(illFormedPath))
        }
      }
      "an absolute file system path to a file that is not a PDF." in { _ =>
        val nonPDFPath: String = s"${os.home.toString}/file.ext"

        the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](nonPDFPath) should have {
          'message(nonPDFFileSystemPath(nonPDFPath))
        }
      }
      "a file system path whose filename" - {
        "is not comprised of three sections delimited by ' - '." in { _ =>
          val fileName: String = s"${os.home.toString}/file - 22722.pdf"

          the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
            'message(incompleteFileName(fileName))
          }
        }
        "is not comprised of three sections delimited by ' - '. (what if intermediate folder have ' - ' as parts of their names)" in { _ =>
          val fileName: String = s"${os.home.toString}/intermediate - folder/file - 22722.pdf"

          the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
            'message(incompleteFileName(fileName))
          }
        }
        "'s 'OperationsDescription' section" - {
          "is empty." in { _ =>
            val fileName: String = s"${os.home.toString}/ - 22722 - 03-10-2022.pdf"

            the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameMissingOperationsDescription(fileName))
            }
          }
          "is empty (Windows)." in { _ =>
            val fileName: String = s"${os.home.toString}/- 22722 - 03-10-2022.pdf"

            the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameMissingOperationsDescription(fileName))
            }
          }
          "has only" - { "" +
            "numbers." in { _ =>
              val fileName: String = s"${os.home.toString}/123456 - 22722 - 03-10-2022.pdf"

              the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
                'message(operationsDescriptionWithOnlyNumbers(fileName))
              }
            }
            "letters." in { _ =>
              val fileName: String = s"${os.home.toString}/compra - 22722 - 03-10-2022.pdf"

              the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
                'message(operationsDescriptionWithOnlyLetters(fileName))
              }
            }
          }
        }
        "'s 'NoteNumber' section" - {
          "is empty." in { _ =>
            val fileName: String = s"${os.home.toString}/compraVALE3 - - 03-10-2022.pdf"

            the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameMissingNoteNumber(fileName))
            }
          }
          "is not numeric." in { _ =>
            val fileName: String = s"${os.home.toString}/compraVALE3 - PETR4 - 03-10-2022.pdf"

            the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameWithNonNumericNoteNumber(fileName))
            }
          }
        }
        "'s 'TradingDate' section" - {
          "is empty." in { _ =>
            val fileName: String = s"${os.home.toString}/compraVALE3 - 22722 - .pdf"

            the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameMissingTradingDate(fileName))
            }
          }
          "is empty (Windows)." in { _ =>
            val fileName: String = s"${os.home.toString}/compraVALE3 - 22722 -.pdf"

            the[RequiredValueMissingException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameMissingTradingDate(fileName))
            }
          }
          "is not a valid date in the format 'dd-MM-yyyy'." in { _ =>
            val fileName: String = s"${os.home.toString}/compraVALE3 - 22722 - 12-20-2022.pdf"

            the[UnexpectedContentValueException] thrownBy PDFBrokerageNotePath.from[FileSystemTest](fileName) should have {
              'message(fileNameWithInvalidTradingDate(fileName))
            }
          }
        }
      }
    }
    "when given a non-empty, well-formed, absolute file system path to a PDF file, be able to provide a 'FileSystemPath' instance that tells that the resource it represents" - {
      "is a file." in { configMap =>
        val fileName: String = "compraVALE3 - 22722 - 03-10-2022.pdf"
        val currentFolder = s"${configMap.getRequired[String]("targetDir")}/test-files"
        val path = s"$currentFolder/$fileName"

        assertWithNonExisting(path)(
          path => PDFBrokerageNotePath.from[FileSystemTest](path).isAFile,
          path => !PDFBrokerageNotePath.from[FileSystemTest](path).isNotAFile
        )
      }
      "is not a folder." in { configMap =>
        val fileName: String = "compraVALE3 - 22722 - 03-10-2022.pdf"
        val currentFolder = s"${configMap.getRequired[String]("targetDir")}/test-files"
        val path = s"$currentFolder/$fileName"

        assertWithNonExisting(path)(
          path => PDFBrokerageNotePath.from[FileSystemTest](path).isNotAFolder,
          path => !PDFBrokerageNotePath.from[FileSystemTest](path).isAFolder
        )
      }
    }
  }

object PDFBrokerageNotePathTest:
  import org.scalatest.Assertions.assert
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemTest.emptyState

  private def assertWithNonExisting(resourceName: String)(assertions: String => FileSystemTest[Boolean]*): Unit =
    assertions.foreach(_ (resourceName).map(assert(_)).run(emptyState).value)

  extension (fileSystemTest: FileSystemTest[Boolean])
    private def unary_! : FileSystemTest[Boolean] = fileSystemTest.map(!_)