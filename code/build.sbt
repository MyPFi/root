import Dependencies.*

ThisBuild / version         := "0.1.0"
ThisBuild / scalaVersion    := "3.3.1"
ThisBuild / organization    := "com.andreidiego"

lazy val root = project
  .in(file("."))
  .aggregate(
    brokerageNotesWorksheetReader,
    brokerageNotesWatcher,
    mpfiStocksCommon,
    mpfiStocksAverageStockPrice,
    mpfiStocksIncomeTaxAtSourceRate,
    mpfiStocksServiceTaxRate,
    mpfiStocksSettlementFeeRate,
    mpfiStocksTradingFeesRate,
    mpfiCreditCard,
    mpfiShopping
  )
  .settings(
    name := "MPFi"
  )

lazy val brokerageNotesWorksheetReader = project
  .in(file("brokeragenotesworksheetreader"))
  .settings(
    name := "MPFi-Stocks-Brokerage Notes - Worksheet Reader",
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += cats,
    libraryDependencies += scalaTest % Test
  )
  .dependsOn(
    mpfiStocksCommon,
    mpfiStocksAverageStockPrice % "compile->compile;test->test",
    mpfiStocksIncomeTaxAtSourceRate % "compile->compile;test->test",
    mpfiStocksServiceTaxRate % "compile->compile;test->test",
    mpfiStocksSettlementFeeRate % "compile->compile;test->test",
    mpfiStocksTradingFeesRate % "compile->compile;test->test",
  )

lazy val brokerageNotesWatcher = project
  .in(file("brokeragenoteswatcher"))
  .settings(
    name := "MPFi-Stocks-Brokerage Notes - Watcher",
    libraryDependencies += fileWatcher,
    libraryDependencies += brokerageNotesPDFReader
  )

lazy val mpfiStocksCommon = project
  .in(file("stocks-common"))
  .settings(
    name := "MPFi-Stocks - Common"
  )

lazy val mpfiStocksAverageStockPrice = project
  .in(file("stocks-averagestockprice"))
  .settings(
    name := "MPFi-Stocks - Average Stock Price"
  )

lazy val mpfiStocksIncomeTaxAtSourceRate = project
  .in(file("stocks-incometaxatsourcerate"))
  .dependsOn(mpfiStocksCommon)
  .settings(
    name := "MPFi-Stocks - Income-Tax-At-Source Rate"
  )

lazy val mpfiStocksServiceTaxRate = project
  .in(file("stocks-servicetaxrate"))
  .settings(
    name := "MPFi-Stocks - Service-Tax Rate"
  )

lazy val mpfiStocksSettlementFeeRate = project
  .in(file("stocks-settlementfeerate"))
  .dependsOn(mpfiStocksCommon)
  .settings(
    name := "MPFi-Stocks - Settlement-Fee Rate"
  )

lazy val mpfiStocksTradingFeesRate = project
  .in(file("stocks-tradingfeesrate"))
  .settings(
    name := "MPFi-Stocks - Trading-Fees Rate"
  )

lazy val mpfiCreditCard = project
  .in(file("creditcard"))
  .settings(
    name := "MPFi-CreditCard",
    libraryDependencies += pdfBox
  )

lazy val mpfiShopping = project
  .in(file("shopping"))
  .settings(
    name := "MPFi-Shopping",
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += scalaTest % Test
  )