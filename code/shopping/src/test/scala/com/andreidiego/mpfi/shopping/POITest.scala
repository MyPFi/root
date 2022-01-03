package com.andreidiego.mpfi.shopping

import java.io.{File, FileOutputStream}

import org.apache.poi.ss.usermodel._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters._

class POITest extends AnyFreeSpec {
  val formatter = new DataFormatter()

  "A Sale" - {
    "must have a" - {
      "1" in {
        val purchasesWorkbook = WorkbookFactory.create(new File("F:/OneDrive/Documentos/Financeiros/Lista de Compras - Copia.xlsx")).asScala
        val purchases = buildPurchaseListFromWorkbook(purchasesWorkbook)
        val products = for {
          purchase <- purchases
          item <- purchase.items
        } yield item.product
        val sortedProducts = products.sortWith(_.name < _.name)
        val distinctSortedProducts = sortedProducts.distinct

        printProducts(sortedProducts, distinctSortedProducts)
        exportDistinctProducts(distinctSortedProducts)
      }
      "2" in {
        val modelWorkbook = WorkbookFactory.create(new File("F:/OneDrive/Documentos/Financeiros/productsToNormalize - Copia.xlsx")).asScala
        val (sortedProductModel, sortedBridgeProducts) = buildProductModelFromWorkbook(modelWorkbook)

        printProductModelAndBridgeProducts(sortedProductModel, sortedBridgeProducts)
      }
      "3" in {
        val purchasesWorkbook = WorkbookFactory.create(new File("F:/OneDrive/Documentos/Financeiros/Lista de Compras - Copia.xlsx")).asScala
        val modelWorkbook = WorkbookFactory.create(new File("F:/OneDrive/Documentos/Financeiros/productsToNormalize - Copia.xlsx")).asScala
        val purchases = buildPurchaseListFromWorkbook(purchasesWorkbook)
        val (sortedProductModel, sortedBridgeProducts) = buildProductModelFromWorkbook(modelWorkbook)

        purchases.foreach(
          _.items.foreach(item =>

            sortedBridgeProducts.find(_._2._1 == item.product) match {
              case Some(bridgeProductTuple) =>

                sortedProductModel.find(_._1 == bridgeProductTuple._1) match {
                  case Some(modelProductTuple) =>
                    item.product(modelProductTuple._2._1)
                    item.addCategory(modelProductTuple._2._2)
                    item.addCategory(modelProductTuple._2._3)

                  case None =>
                    println(s"Product $bridgeProductTuple not found in sortedProductModel")
                }

              case None =>
                println(s"Product ${item.product} not found in sortedBridgeProducts")
            }
          )
        )

        exportPurchases(purchases)
      }
      "4" in {
        //        NewColumnName.values.foreach((value: NewColumnName.Value) => println(value.id))
        exportPurchases(Nil)
      }
    }
  }

  def buildPurchaseListFromWorkbook(purchasesWorkbook: Iterable[Sheet]): List[Purchase] = {
    def buildHeader(row: Array[Cell]): Array[ColumnName.Value] = {
      for (cell <- row) yield if (ColumnName.values.exists(_.toString == cell.getStringCellValue)) ColumnName.withName(cell.getStringCellValue) else ColumnName.Descartavel
    }

    def updatePurchaseWithCellValue(cell: Cell, header: Array[ColumnName.Value], purchase: Purchase, item: Item, product: Product): Unit = {
      import ColumnName._

      val cellValue = formatter.formatCellValue(cell)
      val cellIndex = cell.getColumnIndex

      if (cellIndex < header.length) {

        header(cellIndex) match {
          case Estabelecimento => purchase.establishment(cellValue);
          case ContaCorrente => purchase.paymentMethod(cellValue);
          case Produto => purchase.updateItem(item.product(product.name(cellValue)))
          case Código => purchase.updateItem(item.product(product.id(cellValue)))
          case Marca => purchase.updateItem(item.product(product.brand(cellValue)))
          case Tamanho => purchase.updateItem(item.product(product.size(cellValue)))
          case Qtde => purchase.updateItem(item.qty(cellValue))
          case Preco => purchase.updateItem(item.price(cellValue))
          case Descartavel =>
        }

      } else {
        purchase.updateItem(item.addCategory(cellValue))
      }
    }

    var purchases: List[Purchase] = Nil

    println(s"workbook.size: ${purchasesWorkbook.size}")

    purchasesWorkbook.foreach((sheet: Sheet) => {

      val purchaseDate = sheet.getSheetName
      var purchase = Purchase(purchaseDate)
      purchases = purchase :: purchases
      val header = buildHeader(sheet.getRow(0).asScala.toArray)

      println(s"Sheet: $purchaseDate")
      println(s"\t${header.mkString("\t")}")
      println(s"\t$purchase")

      var itemSequence = 1
      var createNewPurchase = false

      sheet.asScala.foreach((row: Row) => {

        if (row.getRowNum > 0) {
          if (row.getCell(0) != null) {

            if (createNewPurchase) {
              purchase = Purchase(purchaseDate)
              purchases = purchase :: purchases
              itemSequence = 1
              createNewPurchase = false
              println(s"\t$purchase")
            }

            val product = Product()
            val item = Item(itemSequence, product)
            purchase.addItem(item)

            row.asScala.foreach((cell: Cell) => {
              updatePurchaseWithCellValue(cell, header, purchase, item, product)
            })

            println(s"\t\t$item")
            itemSequence += 1

          } else {
            createNewPurchase = true
          }
        }
      })
      println()
    })

    purchases
  }

  def printProducts(sortedProducts: List[Product], distinctSortedProducts: List[Product]): Unit = {
    println(s"How many products? ${sortedProducts.size}")
    println()
    sortedProducts.foreach(println)
    println()
    println("-----------------------------------------------------------")
    println()
    println(s"How many distinct products? ${distinctSortedProducts.size}")
    println()
    distinctSortedProducts.foreach(println)
  }

  def exportDistinctProducts(products: List[Product]): Unit = {
    val workbook = new XSSFWorkbook()
    val sheet = workbook.createSheet()

    for ((product, index) <- products.zipWithIndex) {
      val row = sheet.createRow(index)
      row.createCell(0)
      row.createCell(1)
      row.createCell(2)
      row.createCell(3).setCellValue(product.id)
      row.createCell(4).setCellValue(product.name)
      row.createCell(5).setCellValue(product.brand)
      row.createCell(6).setCellValue(product.size)
    }

    workbook.write(new FileOutputStream("productsToNormalize.xlsx"))
    workbook.close()
  }

  def buildProductModelFromWorkbook(modelWorkbook: Iterable[Sheet]): (ListMap[Int, (Product, String, String)], List[(Int, (Product, String, String))]) = {
    var productModel: Map[Int, (Product, String, String)] = Map()
    var bridgeProducts: List[(Int, (Product, String, String))] = Nil

    modelWorkbook.foreach((sheet: Sheet) => {
      sheet.asScala.foreach((row: Row) => {
        val modelProduct = "x" == formatter.formatCellValue(row.getCell(0))
        val realProduct = "x" == formatter.formatCellValue(row.getCell(1))
        val modelKey = formatter.formatCellValue(row.getCell(2)).toInt
        val id = formatter.formatCellValue(row.getCell(3))
        val name = formatter.formatCellValue(row.getCell(4))
        val brand = formatter.formatCellValue(row.getCell(5))
        val size = formatter.formatCellValue(row.getCell(6))
        val category1 = formatter.formatCellValue(row.getCell(7))
        val category2 = formatter.formatCellValue(row.getCell(8))

        val product = (Product(id, name, brand, size), category1, category2)

        if (modelProduct) {
          productModel += modelKey -> product

          if (realProduct) {
            bridgeProducts = (modelKey, product) :: bridgeProducts
          }

        } else {
          bridgeProducts = (modelKey, product) :: bridgeProducts
        }
      })
    })

    (ListMap(productModel.toSeq.sortBy(_._1): _*), bridgeProducts.sortBy(_._1))
  }

  def printProductModelAndBridgeProducts(sortedProductModel: ListMap[Int, (Product, String, String)], sortedBridgeProducts: List[(Int, (Product, String, String))]): Unit = {
    println(s"sortedProductModel.size: ${sortedProductModel.size}")
    println(s"sortedBridgeProducts.size: ${sortedBridgeProducts.size}")
    println("----------------Product Model------------------")
    sortedProductModel.foreach(println)
    println()
    println("----------------Bridge Products------------------")
    sortedBridgeProducts.foreach(println)
  }

  def exportPurchases(purchases: List[Purchase]): Unit = {
    val workbook = new XSSFWorkbook()
    val sheet = workbook.createSheet()

    var rowNum = 0
    var row = sheet.createRow(rowNum)
    NewColumnName.values.foreach((value: NewColumnName.Value) => row.createCell(value.id).setCellValue(value.toString))

    purchases.foreach((purchase: Purchase) => {
      purchase.items.foreach((item: Item) => {
        rowNum += 1
        var columnIndex = 0
        row = sheet.createRow(rowNum)
        val product = item.product

        row.createCell(columnIndex).setCellValue(purchase.date)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(purchase.establishment)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(product.id)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(product.name)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(product.brand)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(product.size)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(item.qty)
        columnIndex += 1
        row.createCell(columnIndex).setCellValue(item.price)

        item.categories.foreach((category: Category) => {
          columnIndex += 1
          row.createCell(columnIndex).setCellValue(category.category)
        })
      })
    })


    workbook.write(new FileOutputStream("F:/OneDrive/Documentos/Financeiros/Nova Lista de Compras.xlsx"))
    workbook.close()
  }
}

case class Category(category: String) {}

case class Product(var id: String = "", var name: String = "", var brand: String = "", var size: String = "") {

  def id(id: String): Product = {
    this.id = id
    this
  }

  def name(name: String): Product = {
    this.name = name
    this
  }

  def brand(brand: String): Product = {
    this.brand = brand
    this
  }

  def size(size: String): Product = {
    this.size = size
    this
  }
}

case class Item(id: Int, var product: Product, var qty: String = "", var price: String = "", var categories: List[Category] = Nil) {

  def product(product: Product): Item = {
    this.product = product
    this
  }

  def qty(qty: String): Item = {
    this.qty = qty
    this
  }

  def price(price: String): Item = {
    this.price = price
    this
  }

  def addCategory(category: String): Item = {
    categories = Category(category) :: categories
    this
  }
}

case class Purchase(date: String, var establishment: String = "", var items: List[Item] = Nil, var paymentMethod: String = "") {

  def establishment(establishment: String): Unit = this.establishment = establishment

  def addItem(item: Item): Unit = items = item :: items

  def updateItem(item: Item): Unit = items = items.map(i => if (i.id == item.id) item else i)

  def paymentMethod(paymentMethod: String): Unit = this.paymentMethod = paymentMethod
}

object ColumnName extends Enumeration {
  val ContaCorrente: ColumnName.Value = Value("C/C")
  val Estabelecimento, Produto, Código, Marca, Tamanho, Qtde, Preco, Categorias, Descartavel = Value
}

object NewColumnName extends Enumeration {
  val Data, Estabelecimento, Código, Produto, Marca, Tamanho, Qtde, Preço, Categorias = Value
}