import Dependencies._
import sbt.Keys.libraryDependencies

ThisBuild / version       := "0.1.0"
ThisBuild / scalaVersion  := "3.2.1"
ThisBuild / organization  := "com.andreidiego"

lazy val root = project
  .in(file("."))
  .aggregate(mpfiStocks, mpfiShopping)
  .settings(
    name := "MPFi"
  )

lazy val mpfiCreditCard = project
  .in(file("creditcard"))
  //  .dependsOn(root)
  .settings(
    name := "MPFi-CreditCard",
    libraryDependencies += pdfBox
  )

lazy val mpfiShopping = project
  .in(file("shopping"))
  //  .dependsOn(root)
  .settings(
    name := "MPFi-Shopping",
    libraryDependencies += apachePOI,
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += scalaTest % Test
  )

lazy val mpfiStocks = project
  .in(file("stocks"))
  //  .dependsOn(root)
  .settings(
    name                := "MPFi-Stocks",
    Test / testOptions  += Tests.Argument(TestFrameworks.ScalaTest, s"-DtargetDir=${target.value}"),
    libraryDependencies += akkaActor.cross(CrossVersion.for3Use2_13),
    libraryDependencies += alpakka.cross(CrossVersion.for3Use2_13),
    libraryDependencies += apachePOI,
    libraryDependencies += apachePOIOOXML,
    libraryDependencies += cats,
    libraryDependencies += logbackClassic,
    libraryDependencies += osLib,
    libraryDependencies += pdfBox,
    libraryDependencies += scalaParserCombinators.cross(CrossVersion.for3Use2_13),
    libraryDependencies += scallop,
    libraryDependencies += scalaTest % Test
  )