import sbt._

object Dependencies {
  lazy val scallop = "org.rogach" %% "scallop" % "4.1.0"
  lazy val osLib = "com.lihaoyi" %% "os-lib" % "0.8.1"
  lazy val pdfBox = "org.apache.pdfbox" % "pdfbox" % "3.0.0-alpha2"
  lazy val htmlCleaner = "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.26"
  lazy val apachePOI = "org.apache.poi" % "poi" % "5.2.2"
  lazy val apachePOIOOXML = "org.apache.poi" % "poi-ooxml" % "5.2.2"
  lazy val jUnit4 = "junit" % "junit" % "4.13.2"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.11"
  lazy val concordion = "org.concordion" % "concordion" % "3.1.3"
}