import sbt.*

object Dependencies {
  lazy val AkkaVersion    = "2.9.0-M2"

  lazy val apachePOIOOXML = "org.apache.poi"    % "poi-ooxml" % "5.2.4"
  lazy val pdfBox         = "org.apache.pdfbox" % "pdfbox"    % "3.0.0"
  lazy val scalaTest      = "org.scalatest"    %% "scalatest" % "3.3.0-SNAP4"
}