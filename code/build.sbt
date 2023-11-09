import Dependencies.*
import sbt.Keys.libraryDependencies

ThisBuild / version         := "0.1.0"
ThisBuild / scalaVersion    := "3.3.1"
ThisBuild / organization    := "com.andreidiego"
Compile / scalacOptions     ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint"
)
Compile / javacOptions      ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
Test / parallelExecution    := false
Test / testOptions          += Tests.Argument("-oDF")
Test / logBuffered          := false
run / fork                  := false
Global / cancelable         := false // ctrl-c
enablePlugins(JavaAppPackaging, DockerPlugin)
dockerBaseImage             := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername              := sys.props.get("docker.username")
dockerRepository            := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

lazy val root = project
  .in(file("."))
  .aggregate(
    fileSystem,
    extractionGuide,
    brokerageNotesPDFReader,
    brokerageNotesWorksheetReader,
    fileWatcher,
    brokerageNotesWatcher,
    mpfiStocksCommon,
    mpfiStocksAverageStockPrice,
    mpfiStocksIncomeTaxAtSourceRate,
    mpfiStocksServiceTaxRate,
    mpfiStocksSettlementFeeRate,
    mpfiStocksTradingFeesRate,
    mpfiCreditCard,
    mpfiShopping,
    mpfiStocks,
    poi,
    spreadsheetDesign,
    templateInterpreter,
    loggerFolderWatcher
  )
  .settings(
    name := "MPFi"
  )

lazy val fileSystem = project
  .in(file("filesystem"))
  .settings(
    name := "File-System",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, s"-DtargetDir=${target.value}"),
    libraryDependencies += cats,
    libraryDependencies += osLib,
    libraryDependencies += scalaTest % Test
  )

lazy val extractionGuide = project
  .in(file("extractionguide"))
  .settings(
    name := "Extraction-Guide",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, s"-DtargetDir=${target.value}"),
    libraryDependencies += logbackClassic,
    libraryDependencies += scalaParserCombinators,
    libraryDependencies += scalaTest % Test
  )
  .dependsOn(fileSystem % "compile->compile;test->test")

lazy val brokerageNotesPDFReader = project
  .in(file("brokeragenotespdfreader"))
  .settings(
    name := "MPFi-Stocks-Brokerage Notes - PDF Reader",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, s"-DtargetDir=${target.value}"),
    libraryDependencies += cats,
    libraryDependencies += logbackClassic,
    libraryDependencies += pdfBox,
    libraryDependencies += scalaTest % Test
  )
  .dependsOn(fileSystem % "compile->compile;test->test", extractionGuide)

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

lazy val fileWatcher = project
  .in(file("filewatcher"))
  .settings(
    name := "File-Watcher",
    libraryDependencies += akkaPersistence.cross(CrossVersion.for3Use2_13),
    libraryDependencies += akkaPersistenceJDBC.cross(CrossVersion.for3Use2_13),
    libraryDependencies += akkaSerializationJackson.cross(CrossVersion.for3Use2_13),
    libraryDependencies += alpakka.cross(CrossVersion.for3Use2_13),
    libraryDependencies += logbackClassic,
    libraryDependencies += jna,
    libraryDependencies += postgreSQL
  )

lazy val brokerageNotesWatcher = project
  .in(file("brokeragenoteswatcher"))
  .settings(
    name := "MPFi-Stocks-Brokerage Notes - Watcher",
  )
  .dependsOn(brokerageNotesPDFReader, fileWatcher)

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

lazy val mpfiStocks = project
  .in(file("exploratory/stocks"))
  .settings(
    name := "MPFi-Stocks",
    libraryDependencies += scalaTest % Test
  )

lazy val poi = project
  .in(file("exploratory/poi"))
  .settings(
    name := "POI",
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += scalaTest % Test
  )

lazy val spreadsheetDesign = project
  .in(file("exploratory/spreadsheetdesign"))
  .settings(
    name := "Spreadsheet-Design",
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += scalaTest % Test
  )

lazy val templateInterpreter = project
  .in(file("exploratory/templateinterpreter"))
  .settings(
    name := "Template-Interpreter",
    libraryDependencies += osLib,
    libraryDependencies += pdfBox,
    libraryDependencies += scallop
  )

lazy val loggerFolderWatcher = project
  .in(file("exploratory/loggerfolderwatcher"))
  .settings(
    name := "Logger-FolderWatcher",
    libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % "2.8.1").cross(CrossVersion.for3Use2_13),
    libraryDependencies += alpakka.cross(CrossVersion.for3Use2_13),
    libraryDependencies += logbackClassic
  )