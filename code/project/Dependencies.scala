import sbt.*

object Dependencies {
  lazy val AkkaVersion                     = "2.9.0-M2"

  lazy val apachePOIOOXML                  = "org.apache.poi"    % "poi-ooxml"                             % "5.2.4"
  lazy val cats                            = "org.typelevel"    %% "cats-core"                             % "2.10.0"
  lazy val pdfBox                          = "org.apache.pdfbox" % "pdfbox"                                % "3.0.0"
  lazy val mpfiStocksAverageStockPrice     = "com.andreidiego"  %% "mpfi-stocks-average-stock-price"       % "0.1.0"
  lazy val mpfiStocksCommon                = "com.andreidiego"  %% "mpfi-stocks-common"                    % "0.1.0"
  lazy val mpfiStocksIncomeTaxAtSourceRate = "com.andreidiego"  %% "mpfi-stocks-income-tax-at-source-rate" % "0.1.0"
  lazy val mpfiStocksServiceTaxRate        = "com.andreidiego"  %% "mpfi-stocks-service-tax-rate"          % "0.1.0"
  lazy val mpfiStocksSettlementFeeRate     = "com.andreidiego"  %% "mpfi-stocks-settlement-fee-rate"       % "0.1.0"
  lazy val scalaTest                       = "org.scalatest"    %% "scalatest"                             % "3.3.0-SNAP4"
}