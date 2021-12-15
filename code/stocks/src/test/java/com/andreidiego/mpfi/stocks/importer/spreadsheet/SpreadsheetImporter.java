package com.andreidiego.mpfi.stockmarket.importer.spreadsheet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.Iterator;

public class SpreadsheetImporter {

    private static final String spreadsheetPath = "E:/OneDrive/Documentos/Financeiros/Investimentos/Bolsa/Investimentos.xlsx";
    private static FileInputStream inputStream;
    private static Workbook workbook;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        inputStream = new FileInputStream(spreadsheetPath);
        workbook = new XSSFWorkbook(inputStream);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        workbook.close();
        inputStream.close();
    }

    @Test
    public void spreadsheetFileIsAccessible() {
        Sheet firstSheet = workbook.getSheetAt(3);

        for (Row nextRow : firstSheet) {
            Iterator<Cell> cellIterator = nextRow.cellIterator();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();

                switch (cell.getCellTypeEnum()) {
                    case STRING:
                        System.out.print(cell.getStringCellValue());
                        break;
                    case BOOLEAN:
                        System.out.print(cell.getBooleanCellValue());
                        break;
                    case NUMERIC:
                        System.out.print(cell.getNumericCellValue());
                        break;
                    case BLANK:
                        System.out.print(cell.getNumericCellValue());
                        break;
                    case ERROR:
                        System.out.print(cell.getNumericCellValue());
                        break;
                    case FORMULA:
                        System.out.print(cell.getNumericCellValue());
                        break;
                    case _NONE:
                        break;
                }

                System.out.print(" - ");
            }

            System.out.println();
        }

        // assertThat(actual, is());
    }

}
