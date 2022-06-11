package com.andreidiego.mpfi.stocks.exploratory.spreadsheets.design

import java.io.File

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xssf.usermodel.{XSSFWorkbookFactory, XSSFWorkbook}

import scala.Predef.classOf
import scala.jdk.CollectionConverters.*
import scala.language.deprecated.symbolLiterals

import org.scalatest.{BeforeAndAfterAll, Outcome}
import org.scalatest.Inspectors.forAll
import org.scalatest.TryValues.*
import org.scalatest.wordspec.FixtureAnyWordSpec
import org.scalatest.tagobjects.Slow
import org.scalatest.matchers.should.Matchers.*

class POIExcelExtractorTest extends FixtureAnyWordSpec, BeforeAndAfterAll :
  override protected type FixtureParam = POIExcelExtractor

  private val TEST_SPREADSHEET = "spreadsheet.xlsx"
  private var testWorkbook: XSSFWorkbook = _
  
  override protected def beforeAll(): Unit = testWorkbook = XSSFWorkbookFactory.createWorkbook(
    OPCPackage.open(File(getClass.getResource(TEST_SPREADSHEET).getPath))
  )

  override protected def withFixture(test: OneArgTest): Outcome =
    withFixture(test.toNoArgTest(POIExcelExtractor(testWorkbook)))
    
  override protected def afterAll(): Unit = testWorkbook.close()

  "headerOf" should {
    "return all the header cells" when {
      "given a worksheet whose header contains only non-empty cells." in { excelExtractor =>
        val TEST_SHEET = "HeaderWithOnlyNonEmptyCells"

        excelExtractor.headerOf(TEST_SHEET).success.value should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel")
      }
      "given a worksheet whose header contains only non-blank (non-empty and separators) cells." in { excelExtractor =>
        val TEST_SHEET = "HeaderNonEmptyCellsAndSeparator"

        excelExtractor.headerOf(TEST_SHEET).success.value should contain theSameElementsInOrderAs Seq("Data Pregão", "Nota", "Papel", "")
      }
      "given a worksheet whose header contains only non-empty and string-formula cells." in { excelExtractor =>

        excelExtractor.headerOf("StringFormulaInHeader").success.value should contain theSameElementsInOrderAs Seq("Data Pregão", "Data PregãoPapel", "Nota", "Papel")
      }
    }
    "throw an exception" when {
      "given an empty worksheet." in { excelExtractor =>
        val TEST_SHEET = "EmptySheet"

        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header."),
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message(s"Row #0 does not exist in worksheet $TEST_SHEET.")
        )
      }
      "given a worksheet whose header is not in the first line." in { excelExtractor =>
        val TEST_SHEET = "HeaderOutOfPlace"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header."),
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Header is empty.")
        )
      }
      "given a worksheet whose header has only separator (empty but not blank) cells." in { excelExtractor =>
        val TEST_SHEET = "HeaderOnlySeparators"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("An illegal empty cell was found."))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Header is empty.")
        )
      }
      "given a worksheet whose header has a blank (empty but not a separator) cell." in { excelExtractor =>
        val TEST_SHEET = "EmptyCellInHeader"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("An illegal empty cell was found."))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("An illegal blank cell was found.")
        )
      }
      "given a worksheet whose header has a numeric cell." in { excelExtractor =>
        val TEST_SHEET = "NumberInHeader"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("Cannot get a STRING value from a NUMERIC cell"))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Cannot get a STRING value from a NUMERIC cell")
        )
      }
      "given a worksheet whose header has a boolean cell." in { excelExtractor =>
        val TEST_SHEET = "BooleanInHeader"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("Cannot get a STRING value from a BOOLEAN cell"))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Cannot get a STRING value from a BOOLEAN cell")
        )
      }
      "given a worksheet whose header has a date cell." in { excelExtractor =>
        val TEST_SHEET = "DateInHeader"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("Cannot get a STRING value from a NUMERIC cell"))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Cannot get a STRING value from a NUMERIC cell")
        )
      }
      "given a worksheet whose header has a numeric formula cell." in { excelExtractor =>
        val TEST_SHEET = "NumericFormulaInHeader"
        val exception = excelExtractor.headerOf(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"$TEST_SHEET does not seem to have a valid header.")
          // 'cause (new IllegalStateException("Cannot get a STRING value from a NUMERIC formula cell"))
        )

        exception.getCause should have(
          'class(classOf[IllegalStateException]),
          'message("Cannot get a STRING value from a NUMERIC formula cell")
        )
      }
    }
  }
  "line" should {
    "return a sequence of cells" that {
      "is the same size of the header" when {
        "given the number of a line that contains only non-blank cells." in { excelExtractor =>
          val TEST_SHEET = "Notas de Corretagem"
          excelExtractor.line(12)(TEST_SHEET).success.value.size should equal(12)
        }
        "given the number of a line that contains only non-empty cells." in { excelExtractor =>
          val TEST_SHEET = "FiveRegularLines"
          excelExtractor.line(2)(TEST_SHEET).success.value.size should equal(4)
        }
        "given the number of a line that contains both non-empty and empty cells." in { excelExtractor =>
          val TEST_SHEET = "Notas de Corretagem"

          excelExtractor.line(2)(TEST_SHEET).success.value.size should equal(12)
        }
        "given the number of a line that contains only empty cells." in { excelExtractor =>
          val TEST_SHEET = "Notas de Corretagem"

          excelExtractor.line(6)(TEST_SHEET).success.value.size should equal(12)
        }
      }
      "all bellong to the corresponding line number." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("ADDRESS").endsWith(number.toString)
        }
      }
      "all can correctly tell its address." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedAddresses = Iterator(
          "A2", "B2", "C2", "D2", "E2", "F2", "G2", "H2", "I2", "J2", "K2", "L2"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("ADDRESS") should equal(expectedAddresses.next)
        }
      }
      "all can correctly tell its content." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedContents = Iterator(
          "05/11/2008", "1662", "GGBR4", "100", "15,34", "1534,00", "0,12", "0,41", "15,99", "0,80", "", "1550,52"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("CONTENT") should equal(expectedContents.next)
        }
      }
      "all can correctly tell its type." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedTypes = Iterator(
          "NUMERIC", "NUMERIC", "STRING", "NUMERIC", "NUMERIC", "FORMULA", "FORMULA", "FORMULA", "NUMERIC", "NUMERIC", "BLANK", "FORMULA"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("TYPE") should equal(expectedTypes.next)
        }
      }
      "all can correctly tell its font color." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedFontColors = Iterator(
          "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0", "255,0,0"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("FONT-COLOR") should equal(expectedFontColors.next)
        }
      }
      "all can correctly tell its font color even when it's set to automatic." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 187
        val AUTOMATIC = "0,0,0"

        val expectedFontColors = Iterator(
          AUTOMATIC, "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0", "0,0,0"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("FONT-COLOR") should equal(expectedFontColors.next)
        }
      }
      "all can correctly tell its background color." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 130

        val expectedBackgroundColors = Iterator(
          "", "", "", "", "", "", "251,228,213", "251,228,213", "251,228,213", "251,228,213", "251,228,213", ""
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("BACKGROUND-COLOR") should equal(expectedBackgroundColors.next)
        }
      }
      "all can correctly tell any formula that may be attached to it." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 2

        val expectedFormulas = Iterator(
          "", "", "", "", "", "D2*E2", "0.74*(F2/SUM(F2:F4))", "2.51*(F2/SUM(F2:F4))", "", "", "", "F2+G2+H2+I2"
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("FORMULA") should equal(expectedFormulas.next)
        }
      }
      "all can correctly tell any note that may be attached to it." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 112

        val expectedNotes = Iterator(
          "Nota de Corretagem não encontrada em meus arquivos. Informação retirada de histórico de ordens e confirmada com extrato de conta-corrente.",
          "",
          "",
          "",
          "",
          "",
          "",
          "Avell:\nAparentemente, realizada durante leilões de abertura e fechamento. Verificar!", "",
          "",
          "Avell:\nEncontrado no extrato",
          ""
        )

        forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
          cell("NOTE") should equal(expectedNotes.next)
        }
      }
    }
    "return empty lines." in { excelExtractor =>
      val TEST_SHEET = "Notas de Corretagem"
      val number = 6

      forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
        cell("CONTENT") should be(empty)
      }
    }
    "return summary lines." in { excelExtractor =>
      val TEST_SHEET = "Notas de Corretagem"
      val number = 5

      forAll(excelExtractor.line(number)(TEST_SHEET).success.value) { cell ⇒
        cell("TYPE") should (be("FORMULA") or be("BLANK"))
      }
    }
    "throw an exception" when {
      "line number is less than two." in { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 1

        val exception = excelExtractor.line(number)(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalArgumentException]),
          'message(s"Invalid line number: $number. Regular lines start with 2."),
        )

        exception.getCause should have(
          'class(classOf[AssertionError]),
          'message("assertion failed")
        )
      }
      "workbook has no non-empty regular lines." in { excelExtractor =>
        val TEST_SHEET = "HeaderWithOnlyNonEmptyCells"
        val number = 2

        val exception = excelExtractor.line(number)(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalStateException]),
          'message(s"Row #${number - 1} does not exist in worksheet $TEST_SHEET."),
        )
      }
      "line number greater than 'lastLineNumberIn'." ignore { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"
        val number = 603
        val lastLineNumber = 602

        val exception = excelExtractor.line(number)(TEST_SHEET).failure.exception

        exception should have(
          'class(classOf[IllegalArgumentException]),
          'message(
            s"Invalid line number: $number. Last line number in $TEST_SHEET is $lastLineNumber."
          )
        )

        exception.getCause should have(
          'class(classOf[AssertionError]),
          'message("assertion failed")
        )
      }
    }
  }
  "lastLineNumberIn" should {
    "return 0" when {
      "given an empty worksheet." in { excelExtractor =>
        val TEST_SHEET = "EmptySheet"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(0)
      }
    }
    "return 1" when {
      "given a worksheet containing only the header." in { excelExtractor =>
        val TEST_SHEET = "HeaderWithOnlyNonEmptyCells"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(1)
      }
      "given a worksheet containing only one empty line right after header." in { excelExtractor =>
        val TEST_SHEET = "OnlyOneEmptyLineAfterHeader"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(1)
      }
      "given a worksheet containing only two empty lines right after header." in { excelExtractor =>
        val TEST_SHEET = "TwoEmptyLines"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(1)
      }
      "given a worksheet containing only three empty lines right after header." in { excelExtractor =>
        val TEST_SHEET = "ThreeEmptyLines"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(1)
      }
      "given a worksheet containing more than one empty line between the header and the first regular line." in { excelExtractor =>
        val TEST_SHEET = "IrregularEmptyLinesAtTheTop"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(1)
      }
    }
    "return 2" when {
      "given a worksheet containing only one regular line." in { excelExtractor =>
        val TEST_SHEET = "OneRegularLine"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(2)
      }
      "given a worksheet containing a regular line followed by an empty line." in { excelExtractor =>
        val TEST_SHEET = "ThirdLineEmpty"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(2)
      }
      "given a worksheet containing a regular line followed by two empty lines." in { excelExtractor =>
        val TEST_SHEET = "FourthAndThirdLineEmpty"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(2)
      }
    }
    "return 3" when {
      "given a worksheet containing only one regular line after one empty line." in { excelExtractor =>
        val TEST_SHEET = "OneRegularLineAfterOneEmpty"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(3)
      }
      "given a worksheet containing an empty line followed by a regular line followed by an empty line." in { excelExtractor =>
        val TEST_SHEET = "FourthLineEmpty"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(3)
      }
    }
    "return 4" when {
      "given a worksheet containing one empty and two regular lines after the header." in { excelExtractor =>
        val TEST_SHEET = "FourthAndThirdRegularLines"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(4)
      }
    }
    "return 5" when {
      "given a worksheet containing four regular lines after the header." in { excelExtractor =>
        val TEST_SHEET = "FiveRegularLines"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(5)
      }
      "given a worksheet containing an irregular three empty-line block between its last non-empty line and the four regular lines after the header." in { excelExtractor =>
        val TEST_SHEET = "IrregularEmptyLineInterval"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(5)
      }
    }
    "return the number of the last non-empty line before an irregular three empty-line block" when {
      "given a worksheet containing non-empty regular (non-header) lines." taggedAs Slow ignore { excelExtractor =>
        val TEST_SHEET = "Notas de Corretagem"

        excelExtractor.lastLineNumberIn(TEST_SHEET) should be(602)
      }
    }
  }
  "lastColumnIndexIn" should {
    "" that {
      "" when {
        "" ignore { excelExtractor =>
        }
      }
    }
  }
  "group" should {
    "" that {
      "" when {
        "" ignore { excelExtractor =>
        }
      }
    }
  }
  "double-checker" should {
    "" that {
      "" when {
        "" ignore { excelExtractor =>
        }
      }
    }
  }