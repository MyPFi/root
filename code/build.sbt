import Dependencies.*

ThisBuild / version         := "0.1.0"
ThisBuild / scalaVersion    := "3.3.1"
ThisBuild / organization    := "com.andreidiego"

lazy val root = project
  .in(file("."))
  .aggregate(
    brokerageNotesWorksheetReader,
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
    libraryDependencies += mpfiStocksAverageStockPrice % "compile->compile;test->test",
    libraryDependencies += mpfiStocksCommon,
    libraryDependencies += mpfiStocksIncomeTaxAtSourceRate % "compile->compile;test->test",
    libraryDependencies += mpfiStocksServiceTaxRate % "compile->compile;test->test",
    libraryDependencies += scalaTest % Test
  )
  .dependsOn(
    mpfiStocksSettlementFeeRate % "compile->compile;test->test",
    mpfiStocksTradingFeesRate % "compile->compile;test->test",
  )

lazy val mpfiStocksSettlementFeeRate = project
  .in(file("stocks-settlementfeerate"))
  .settings(
    name := "MPFi-Stocks - Settlement-Fee Rate",
    libraryDependencies += mpfiStocksCommon
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