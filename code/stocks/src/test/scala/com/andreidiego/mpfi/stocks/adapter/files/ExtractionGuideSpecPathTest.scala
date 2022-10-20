package com.andreidiego.mpfi.stocks.adapter.files

import scala.annotation.experimental
import org.scalatest.freespec.FixtureAnyFreeSpec
import org.scalatest.fixture.ConfigMapFixture

@experimental
class ExtractionGuideSpecPathTest extends FixtureAnyFreeSpec, ConfigMapFixture:
  import language.deprecated.symbolLiterals
  import unsafeExceptions.canThrowAny
  import org.scalatest.matchers.should.Matchers.*
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemTest.StateFileSystem
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathException.*
  import com.andreidiego.mpfi.stocks.adapter.files.FileSystemPathMessages.*
  import ExtractionGuideSpecPathMessages.*

  "A 'ExtractionGuideSpecPath' should" - {
    "be built from a string representing the path to a TXT file." in { _ =>
      val txtPath: String = s"${os.home.toString}/file.txt"

      "ExtractionGuideSpecPath.from(txtPath)" should compile
    }
    "fail to be built when given" - {
      "an empty string." in { _ =>
        the[RequiredValueMissingException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem]("") should have {
          'message(fileSystemPathMissing)
        }
      }
      "a relative file system path." in { _ =>
        val relativePath = "folder/file.txt"

        the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](relativePath) should have {
          'message(relativeFileSystemPathNotAllowed(relativePath))
        }
      }
      /*
      TODO This test will probably fail if run on Linux since it looks like almost everything is
       possible when it comes to naming files ignore Linux  - although I couldn't find an authoritative
       source for what is acceptable and what is not.
      */
      "an ill-formed file system path." in { _ =>
        val illFormedPath = s"${os.home}/?"

        the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](illFormedPath) should have {
          'message(invalidFileSystemPath(illFormedPath))
        }
      }
      "an absolute file system path to a file that is not a TXT." in { _ =>
        val nonTXTPath: String = s"${os.home.toString}/file.ext"

        the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](nonTXTPath) should have {
          'message(nonTXTFileSystemPath(nonTXTPath))
        }
      }
      "a file system path whose filename" - {
        "is comprised of sections delimited by '-' in a number" - {
          "lower than three" - {
            "." in { _ =>
              val fileName: String = s"${os.home.toString}/nota_corretagem-modal_mais.txt"

              the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
                'message(incompleteFileName(fileName))
              }
            }
            "even if intermediate folders have '-' as part of their names." in { _ =>
              val fileName: String = s"${os.home.toString}/intermediate-folder/nota_corretagem-modal_mais.txt"

              the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
                'message(incompleteFileName(fileName))
              }
            }
          }
          "or, higher than three." in { _ =>
            val fileName: String = s"${os.home.toString}/nota-corretagem-modal-mais.txt"

            the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
              'message(invalidFileNameStructure(fileName))
            }
          }
        }
        "'s 'DocumentType' section" - {
          "is empty" in { _ =>
            val fileName: String = s"${os.home.toString}/-modal_mais-v1.txt"

            the[RequiredValueMissingException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
              'message(fileNameMissingDocumentType(fileName))
            }
          }
          "or, has characters other than letters and the '_' sign." in { _ =>
            val fileName: String = s"${os.home.toString}/nota_corretagem2-modal_mais-v1.txt"

            the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
              'message(documentTypeWithInvalidCharacters(fileName))
            }
          }
        }
        "'s 'DocumentIssuer' section is empty." in { _ =>
          val fileName: String = s"${os.home.toString}/nota_corretagem--v1.txt"

          the[RequiredValueMissingException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
            'message(fileNameMissingDocumentIssuer(fileName))
          }
        }
        "'s 'DocumentVersion' section" - {
          "is empty" in { _ =>
            val fileName: String = s"${os.home.toString}/nota_corretagem-modal_mais-.txt"

            the[RequiredValueMissingException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
              'message(fileNameMissingDocumentVersion(fileName))
            }
          }
          "or, either" - {
            "doesn't start with the letter 'v'" in { _ =>
              val fileName: String = s"${os.home.toString}/nota_corretagem-modal_mais-1.txt"

              the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
                'message(fileNameWithInvalidDocumentVersion(fileName))
              }
            }
            "or, doesn't finish with a number." in { _ =>
              val fileName: String = s"${os.home.toString}/nota_corretagem-modal_mais-vI.txt"

              the[UnexpectedContentValueException] thrownBy ExtractionGuideSpecPath.from[StateFileSystem](fileName) should have {
                'message(fileNameWithInvalidDocumentVersion(fileName))
              }
            }
          }
        }
      }
    }
  }