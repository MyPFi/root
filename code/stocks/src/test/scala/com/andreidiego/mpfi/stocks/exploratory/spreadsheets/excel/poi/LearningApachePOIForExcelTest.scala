package com.andreidiego.mpfi.stocks.exploratory.spreadsheets.excel.poi

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.CellType.{BLANK, FORMULA, NUMERIC, STRING}
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
import org.apache.poi.util.LocaleUtil
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFCellStyle, XSSFColor}
import org.scalatest.Outcome
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAnyWordSpec

import java.io.File
import java.util.Locale
import scala.jdk.CollectionConverters.*
import scala.util.Try

class LearningApachePOIForExcelTest extends FixtureAnyWordSpec with Matchers :
  type FixtureParam = Sheet

  val TEST_SPREADSHEET = "/com/andreidiego/mpfi/stocks/exploratory/spreasheets/excel/poi/spreadsheet.xlsx"
  val TRADING_DATE_COLUMN_INDEX = 0
  val TRADING_DATE_ENCODED = "40177"
  val TRADING_DATE_US_FORMAT = "12/30/09"
  val TRADING_DATE_BR_FORMAT = "30/12/2009"
  val TRADING_DATE_NOTE = "Nota de Corretagem não encontrada em meus arquivos. Informação retirada de histórico de ordens e confirmada com extrato de conta-corrente."
  val SHORT_DATE = 14
  val PT_BR_DATE_FORMAT = "dd/MM/yyyy"
  val BROKERAGE_NOTE_COLUMN_INDEX = 1
  val BROKERAGE_NOTE = "812"
  val STOCK_SYMBOL_COLUMN_INDEX = 2
  val STOCK_SYMBOL = "MMXM3"
  val QTY_COLUMN_INDEX = 3
  val PRICE_COLUMN_INDEX = 4
  val PRICE = "12.35"
  val VOLUME_COLUMN_INDEX = 5
  val VOLUME = "4940"
  val VOLUME_FORMULA = "D2*E2"
  val SETTLEMENT_FEE_COLUMN_INDEX = 6
  val OPERATION_FEES_COLUMN_INDEX = 7
  val OPERATION_FEES_FORMULA = "F2*0.007%"
  val BROKERAGE_FEE_COLUMN_INDEX = 8
  val SERVICES_TAX_COLUMN_INDEX = 9
  val TAX_DEDUCTED_AT_SOURCE_COLUMN_INDEX = 10
  val TOTAL_COLUMN_INDEX = 11
  val EMPTY_COLUMN_INDEX = 12
  val STRING_CONCAT_FORMULA_COLUMN_INDEX = 13
  val STRING_CONCAT_FORMULA: String = STOCK_SYMBOL + "812"
  val CELL_FILL_FOREGROUND_COLOR: Array[Byte] = Array(251, 228, 213).map(_.toByte)
  val FONT_COLOR: Array[Byte] = Array(68, 114, 196).map(_.toByte)

  val formatter = new DataFormatter(LocaleUtil.getUserLocale)

  override protected def withFixture(test: OneArgTest): Outcome = {
    //    OPCPackage pkg = OPCPackage.open(new File("file.xlsx"));
    //    XSSFWorkbook wb = new XSSFWorkbook(pkg);
    //    ....
    //    pkg.close();

    val testWorkbook = WorkbookFactory.create(
      new File(getClass.getResource(TEST_SPREADSHEET).getPath)
    )
    val sheet = testWorkbook.getSheetAt(0)

    try withFixture(test.toNoArgTest(sheet))
    finally testWorkbook.close()
  }

  def cellAt(index: Int)(sheet: Sheet): XSSFCell =
    sheet.getRow(1).getCell(index, CREATE_NULL_AS_BLANK).asInstanceOf[XSSFCell]

  "cell.getRawValue" should {
    "return the numeric encoded value for cells formatted as dates" in { sheet =>
      val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
      assume(tradingDateCell.getCellType == NUMERIC)
      assume(DateUtil.isCellDateFormatted(tradingDateCell))

      tradingDateCell.getRawValue should be(TRADING_DATE_ENCODED)
    }
    "return the number as displayed in Excel for numeric cells" in { sheet =>
      val brokerageNoteCell = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)
      assume(brokerageNoteCell.getCellType == NUMERIC)
      assume(brokerageNoteCell.getCellStyle.getDataFormat == 0)

      brokerageNoteCell.getRawValue should be(BROKERAGE_NOTE)
    }
    "return 0 for string cells" in { sheet =>
      val stockSymbolCell = cellAt(STOCK_SYMBOL_COLUMN_INDEX)(sheet)
      assume(stockSymbolCell.getCellType == STRING)

      stockSymbolCell.getRawValue should be("0")
    }
    "return 'null' for empty cells" in { sheet =>
      val emptyCell = cellAt(EMPTY_COLUMN_INDEX)(sheet)
      assume(emptyCell.getCellType == BLANK)
      assume(emptyCell.getStringCellValue.isEmpty)

      emptyCell.getRawValue should be(null)
    }
    "return the numeric value for numeric cells formatted as currency" in { sheet =>
      val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)
      assume(priceCell.getCellType == NUMERIC)
      assume(priceCell.getCellStyle.getDataFormat == 8)

      priceCell.getRawValue should be(PRICE)
    }
    "return the numeric value for formula cells formatted as currency" in { sheet =>
      val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
      assume(volumeCell.getCellType == FORMULA)
      assume(volumeCell.getCellStyle.getDataFormat == 8)

      volumeCell.getRawValue should be(VOLUME)
    }
    "return the resulting string for formula cells that operate on strings" in { sheet =>
      val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
      assume(stringConcatFormulaCell.getCellType == FORMULA)
      assume(stringConcatFormulaCell.getCachedFormulaResultType == STRING)

      stringConcatFormulaCell.getRawValue should be(STRING_CONCAT_FORMULA)
    }
  }

  "cell.getStringCellValue" should {
    "return the cell contents" when {
      "the cell contains some non-empty string" in { sheet =>
        val stockSymbolCell = cellAt(STOCK_SYMBOL_COLUMN_INDEX)(sheet)
        assume(stockSymbolCell.getCellType == STRING)

        stockSymbolCell.getStringCellValue should be(STOCK_SYMBOL)
      }
      "the cell contains an empty string" in { sheet =>
        val emptyCell = cellAt(EMPTY_COLUMN_INDEX)(sheet)
        assume(emptyCell.getCellType == CellType.BLANK)

        emptyCell.getStringCellValue should be("")
      }
      "the cell contains a formula that evaluates to a string result" in { sheet =>
        val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
        assume(stringConcatFormulaCell.getCellType == FORMULA)
        assume(stringConcatFormulaCell.getCachedFormulaResultType == STRING)

        stringConcatFormulaCell.getStringCellValue should be(STRING_CONCAT_FORMULA)
      }
    }
    "throw an exception" when {
      "the cell represents a date" in { sheet =>
        val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
        assume(tradingDateCell.getCellType == NUMERIC)
        assume(DateUtil.isCellDateFormatted(tradingDateCell))

        an[IllegalStateException] should be thrownBy tradingDateCell.getStringCellValue
      }
      "the cell represents a number" in { sheet =>
        val brokerageNoteCell = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)
        assume(brokerageNoteCell.getCellType == NUMERIC)
        assume(brokerageNoteCell.getCellStyle.getDataFormat == 0)

        an[IllegalStateException] should be thrownBy brokerageNoteCell.getStringCellValue
      }
      "the cell represents a currency" in { sheet =>
        val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)
        assume(priceCell.getCellType == NUMERIC)
        assume(priceCell.getCellStyle.getDataFormatString.contains("$"))

        an[IllegalStateException] should be thrownBy priceCell.getStringCellValue
      }
      "the cell contains a numeric formula" in { sheet =>
        val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
        assume(volumeCell.getCellType == FORMULA)
        assume(volumeCell.getCachedFormulaResultType == NUMERIC)

        an[IllegalStateException] should be thrownBy volumeCell.getStringCellValue
      }
    }
  }

  "formatter.formatCellValue" should {
    "return short dates in en_US format if a specific format is not set" in { sheet =>
      val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
      assume(tradingDateCell.getCellStyle.getDataFormat == SHORT_DATE)

      formatter.formatCellValue(tradingDateCell) should be(TRADING_DATE_US_FORMAT)
    }
    "return short dates in pt_BR format if that format is set" in { sheet =>
      val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
      assume(tradingDateCell.getCellStyle.getDataFormat == SHORT_DATE)

      formatter.addFormat("m/d/yy", new java.text.SimpleDateFormat(PT_BR_DATE_FORMAT))

      formatter.formatCellValue(tradingDateCell) should be(TRADING_DATE_BR_FORMAT)
    }
    "return the formula for formula cells" in { sheet =>
      val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
      assume(volumeCell.getCellType == FORMULA)

      formatter.formatCellValue(volumeCell) should be(VOLUME_FORMULA)
    }
  }

  "cell.getCellComment" should {
    "return the attached note when the cell has one" in { sheet =>
      val cellWithNote = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)

      cellWithNote.getCellComment.getString.toString should be(TRADING_DATE_NOTE)
    }
    "return 'null' when the cell has no notes attached to it" in { sheet =>
      val cellWithNoNote = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)

      cellWithNoNote.getCellComment should be(null)
    }
  }

  "cell.getCellStyle.getFillForegroundColorColor.getRGBWithTint" should {
    "return the foreground color when the cell has one" in { sheet =>
      val cellWithForegroundColor = cellAt(SETTLEMENT_FEE_COLUMN_INDEX)(sheet)

      cellWithForegroundColor.getCellStyle.getFillForegroundColorColor.getRGBWithTint should be(CELL_FILL_FOREGROUND_COLOR)
    }
    "return 'null' when the cell has no foreground color" in { sheet =>
      val cellWithNoForegroundColor = cellAt(VOLUME_COLUMN_INDEX)(sheet)

      cellWithNoForegroundColor.getCellStyle.getFillForegroundColorColor should be(null)
    }
  }

  "cell.getCellStyle.getFont.getXSSFColor.getRGBWithTint" should {
    "return the cell's font color" in { sheet =>
      val anyCell = cellAt(SETTLEMENT_FEE_COLUMN_INDEX)(sheet)

      anyCell.getCellStyle.getFont.getXSSFColor.getRGBWithTint should be(FONT_COLOR)
    }
  }

  "cell.getCellType" should {
    "return 'NUMERIC'" when {
      "cell content is a date" in { sheet =>
        val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)

        tradingDateCell.getCellType should be(NUMERIC)
      }
      "cell content is an integer" in { sheet =>
        val brokerageNoteCell = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)

        brokerageNoteCell.getCellType should be(NUMERIC)
      }
      "cell content is a currency" in { sheet =>
        val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)

        priceCell.getCellType should be(NUMERIC)
      }
    }
    "return 'STRING' when cell content is alphanumeric" in { sheet =>
      val stockSymbolCell = cellAt(STOCK_SYMBOL_COLUMN_INDEX)(sheet)
      assume(stockSymbolCell.getCellType == STRING)
      assume(stockSymbolCell.getRawValue == "0")

      stockSymbolCell.getCellType should be(STRING)
    }
    "return 'BLANK' when cell is empty" in { sheet =>
      val emptyCell = cellAt(EMPTY_COLUMN_INDEX)(sheet)
      assume(emptyCell.getRawValue == null)

      emptyCell.getCellType should be(BLANK)
    }
    "return 'FORMULA'" when {
      "cell content is a formula resulting in a number" in { sheet =>
        val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
        assume(volumeCell.getCellFormula.nonEmpty)
        val rawValue = volumeCell.getRawValue
        assume(rawValue.toIntOption.isDefined || rawValue.toDoubleOption.isDefined)
        assume(volumeCell.getCachedFormulaResultType == NUMERIC)

        volumeCell.getCellType should be(FORMULA)
      }
      "cell content is a formula resulting in a string" in { sheet =>
        val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
        val rawValue = stringConcatFormulaCell.getRawValue
        assume(rawValue.toIntOption.isEmpty && rawValue.toDoubleOption.isEmpty)

        stringConcatFormulaCell.getCellType should be(FORMULA)
      }
    }
  }

  "DateUtil.isCellDateFormatted(cell)" should {
    "be 'true'" when {
      "cell content is a short date" in { sheet =>
        val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
        assume(tradingDateCell.getCellType == NUMERIC)
        assume(tradingDateCell.getCellStyle.getDataFormat == 14)

        DateUtil.isCellDateFormatted(tradingDateCell) should be(true)
      }
    }
    "be 'false'" when {
      "cell content is a regular number" in { sheet =>
        val brokerageNoteCell = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)
        assume(brokerageNoteCell.getCellType == NUMERIC)
        assume(brokerageNoteCell.getCellStyle.getDataFormat == 0)

        DateUtil.isCellDateFormatted(brokerageNoteCell) should be(false)
      }
      "cell content is empty" in { sheet =>
        val emptyCell = cellAt(EMPTY_COLUMN_INDEX)(sheet)
        assume(emptyCell.getCellType == BLANK)
        assume(emptyCell.getRawValue == null)

        DateUtil.isCellDateFormatted(emptyCell) should be(false)
      }
      "cell content is a currency" in { sheet =>
        val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)
        assume(priceCell.getCellType == NUMERIC)
        assume(priceCell.getCellStyle.getDataFormat == 8)

        DateUtil.isCellDateFormatted(priceCell) should be(false)
      }
      "cell content is a formula that results in a number" in { sheet =>
        val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
        assume(volumeCell.getCellType == FORMULA)
        assume(volumeCell.getCachedFormulaResultType == NUMERIC)

        DateUtil.isCellDateFormatted(volumeCell) should be(false)
      }
    }
    "throw an exception" when {
      "cell content is alphanumeric" in { sheet =>
        val stockSymbolCell = cellAt(STOCK_SYMBOL_COLUMN_INDEX)(sheet)
        assume(stockSymbolCell.getCellType == STRING)
        assume(stockSymbolCell.getRawValue == "0")

        an[IllegalStateException] should be thrownBy DateUtil.isCellDateFormatted(stockSymbolCell)
      }
      "cell content is a formula that results in a string" in { sheet =>
        val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
        assume(stringConcatFormulaCell.getCellType == FORMULA)
        assume(stringConcatFormulaCell.getCachedFormulaResultType == STRING)

        an[IllegalStateException] should be thrownBy DateUtil.isCellDateFormatted(stringConcatFormulaCell)
      }
    }
  }

  "cell.getCellStyle.getDataFormat" should {
    "be '14' for short dates" in { sheet =>
      val tradingDateCell = cellAt(TRADING_DATE_COLUMN_INDEX)(sheet)
      assume(tradingDateCell.getCellType == NUMERIC)
      assume(tradingDateCell.getCellStyle.getDataFormatString == "m/d/yy")

      tradingDateCell.getCellStyle.getDataFormat should be(14)
    }
    "be '8' for currencies" in { sheet =>
      val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)
      assume(priceCell.getCellType == NUMERIC)
      assume(priceCell.getCellStyle.getDataFormatString.contains("$"))

      priceCell.getCellStyle.getDataFormat should be(8)
    }
    "be '0'" when {
      "cell content is a regular number" in { sheet =>
        val brokerageNoteCell = cellAt(BROKERAGE_NOTE_COLUMN_INDEX)(sheet)
        assume(brokerageNoteCell.getCellType == NUMERIC)
        assume(brokerageNoteCell.getCellStyle.getDataFormatString == "General")

        brokerageNoteCell.getCellStyle.getDataFormat should be(0)
      }
      "cell content is alphanumeric" in { sheet =>
        val stockSymbolCell = cellAt(STOCK_SYMBOL_COLUMN_INDEX)(sheet)
        assume(stockSymbolCell.getCellType == STRING)
        assume(stockSymbolCell.getRawValue == "0")

        stockSymbolCell.getCellStyle.getDataFormat should be(0)
      }
      "cell is empty" in { sheet =>
        val emptyCell = cellAt(EMPTY_COLUMN_INDEX)(sheet)
        assume(emptyCell.getRawValue == null)

        emptyCell.getCellStyle.getDataFormat should be(0)
      }
      "cell is a formula that results in a string" in { sheet =>
        val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
        assume(stringConcatFormulaCell.getCellType == FORMULA)
        assume(stringConcatFormulaCell.getCachedFormulaResultType == STRING)

        stringConcatFormulaCell.getCellStyle.getDataFormat should be(0)
      }
    }
  }

  "cell.getCachedFormulaResultType" should {
    "be 'NUMERIC' for formula cells resulting in a number" in { sheet =>
      val volumeCell = cellAt(VOLUME_COLUMN_INDEX)(sheet)
      assume(volumeCell.getCellType == FORMULA)
      val rawValue = volumeCell.getRawValue
      assume(rawValue.toIntOption.isDefined || rawValue.toDoubleOption.isDefined)

      volumeCell.getCachedFormulaResultType should be(NUMERIC)
    }
    "be 'String' for formula cells resulting in a string" in { sheet =>
      val stringConcatFormulaCell = cellAt(STRING_CONCAT_FORMULA_COLUMN_INDEX)(sheet)
      assume(stringConcatFormulaCell.getCellType == FORMULA)
      val rawValue = stringConcatFormulaCell.getRawValue
      assume(rawValue.toIntOption.isEmpty && rawValue.toDoubleOption.isEmpty)

      stringConcatFormulaCell.getCachedFormulaResultType should be(STRING)
    }
    "throw an exception for non-formula cells" in { sheet =>
      val priceCell = cellAt(PRICE_COLUMN_INDEX)(sheet)
      assume(priceCell.getCellType != FORMULA)

      an[IllegalStateException] should be thrownBy priceCell.getCachedFormulaResultType
    }
  }

  "Calling several methods in Cell for different cells in the spreadsheet" should {
    "display the results of calling each one for each cell" in { sheet =>
      val cellFunctions = List[(String, XSSFCell => Any)](
        ("cell.getCellType", _.getCellType),
        ("cell.get_CellValue", cell => cell.getCellType match {
          case NUMERIC => cell.getNumericCellValue
          case FORMULA if cell.getCachedFormulaResultType == NUMERIC => cell.getNumericCellValue
          case FORMULA => cell.getStringCellValue
          case STRING => cell.getStringCellValue
          case BLANK => cell.getStringCellValue
          case _ =>
        }),
        ("cell.getRawValue", _.getRawValue),
        ("cell.getCachedFormulaResultType", cell =>
          Try {
            cell.getCachedFormulaResultType
          }.getOrElse("No cached results.")
        ),
        ("formatter.formatCellValue", formatter.formatCellValue(_)),
        ("cell.getCellStyle.getDataFormat", _.getCellStyle.getDataFormat),
        ("cell.getCellStyle.getDataFormatString", _.getCellStyle.getDataFormatString),
        ("cell.getCellFormula", cell =>
          Try {
            cell.getCellFormula
          }.getOrElse("Not a formula")
        ),
        ("cell.getArrayFormulaRange", cell =>
          Try {
            cell.getArrayFormulaRange
          }.getOrElse("Not in an array formula range")
        ),
        ("cell.getCellComment", cell => Option(cell.getCellComment)
          .map(_.getString)
          .getOrElse("No note")
        ),
        ("cell.getCellStyle.getFillBackgroundColor", _.getCellStyle.getFillBackgroundColor),
        ("IndexedColors.fromInt(cell.getCellStyle.getFillBackgroundColor)", cell => IndexedColors.fromInt(cell.getCellStyle.getFillBackgroundColor)
        ),
        ("cell.getCellStyle.getFillBackgroundXSSFColor.getARGBHex", cell => Option(cell.getCellStyle.getFillBackgroundXSSFColor)
          .map(_.getARGBHex)
          .getOrElse("No FillBackgroundXSSFColor")
        ),
        ("cell.getCellStyle.getFillBackgroundColorColor.getARGBHex", cell => Option(cell.getCellStyle.getFillBackgroundColorColor)
          .map(_.getARGBHex)
          .getOrElse("No FillBackgroundColorColor")
        ),
        ("IndexedColors.fromInt(cell.getCellStyle.getFillForegroundColor)", cell => IndexedColors.fromInt(cell.getCellStyle.getFillForegroundColor)
        ),
        ("cell.getCellStyle.getFillForegroundXSSFColor.getRGB", cell => Option(cell.getCellStyle.getFillForegroundXSSFColor)
          .map(_.getRGB.map(b => b & 0xFF).mkString(","))
          .getOrElse("No FillForegroundXSSFColor")
        ),
        ("cell.getCellStyle.getFillForegroundXSSFColor.getARGBHex", cell => Option(cell.getCellStyle.getFillForegroundXSSFColor)
          .map(_.getARGBHex)
          .getOrElse("No FillForegroundXSSFColor")
        ),
        ("cell.getCellStyle.getFillForegroundColorColor.getRGB", cell => Option(cell.getCellStyle.getFillForegroundColorColor)
          .map(_.getRGB.map(b => b & 0xFF).mkString(","))
          .getOrElse("No FillForegroundColorColor")
        ),
        ("cell.getCellStyle.getFillForegroundColorColor.getARGBHex", cell => Option(cell.getCellStyle.getFillForegroundColorColor)
          .map(_.getARGBHex)
          .getOrElse("No FillForegroundColorColor")
        ),
        ("cell.getCellStyle.getFillForegroundColorColor.isThemed", cell => Option(cell.getCellStyle.getFillForegroundColorColor)
          .map(_.isThemed)
          .getOrElse("No FillForegroundColorColor")
        ),
        ("cell.getCellStyle.getFillForegroundColorColor.getTheme", cell => Option(cell.getCellStyle.getFillForegroundColorColor)
          .map(_.getTheme)
          .getOrElse("No FillForegroundColorColor")
        ),
        ("cell.getCellStyle.getFillForegroundColorColor.getRGBWithTint", cell => Option(cell.getCellStyle.getFillForegroundColorColor)
          .map(_.getRGBWithTint.map(b => b & 0xFF).mkString(","))
          .getOrElse("No FillForegroundColorColor")
        ),
        ("cell.getCellStyle.getFont.getColor", _.getCellStyle.getFont.getColor),
        ("IndexedColors.fromInt(cell.getCellStyle.getFont.getColor)", cell => IndexedColors.fromInt(cell.getCellStyle.getFont.getColor)
        ),
        ("cell.getCellStyle.getFont.getThemeColor", _.getCellStyle.getFont.getThemeColor),
        ("cell.getCellStyle.getFont.getXSSFColor.getRGB", _.getCellStyle.getFont.getXSSFColor.getRGB
          .map(b => b & 0xFF)
          .mkString(",")
        ),
        ("cell.getCellStyle.getFont.getXSSFColor.getARGB", _.getCellStyle.getFont.getXSSFColor.getARGB
          .map(b => b & 0xFF)
          .mkString(",")
        ),
        ("cell.getCellStyle.getFont.getXSSFColor.getARGBHex", _.getCellStyle.getFont.getXSSFColor.getARGBHex),
        ("cell.getCellStyle.getFont.getXSSFColor.isThemed", _.getCellStyle.getFont.getXSSFColor.isThemed),
        ("cell.getCellStyle.getFont.getXSSFColor.getTheme", _.getCellStyle.getFont.getXSSFColor.getTheme),
        ("cell.getCellStyle.getFont.getXSSFColor.getRGBWithTint", _.getCellStyle.getFont.getXSSFColor.getRGBWithTint
          .map(b => b & 0xFF)
          .mkString(",")
        ),
        ("CellTypes", cell => CellType.values().mkString("Array(", ", ", ")"))
      )

      for {
        cellFunction <- cellFunctions
        i <- 0 to 13
        cell = cellAt(i)(sheet)
      } {
        println(s"${cellFunction._1}: ${cellFunction._2(cell)}")
        if i == 13 then println()
      }
    }
  }