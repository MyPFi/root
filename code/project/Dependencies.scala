import sbt.*

object Dependencies {
  lazy val AkkaVersion              = "2.9.0-M2"

  lazy val apachePOIOOXML           = "org.apache.poi"      % "poi-ooxml"                              % "5.2.4"
  lazy val brokerageNotesPDFReader  = "com.andreidiego"    %% "mpfi-stocks-brokerage-notes-pdf-reader" % "0.1.0"
  lazy val cats                     = "org.typelevel"      %% "cats-core"                              % "2.10.0"
  lazy val fileWatcher              = "com.andreidiego"    %% "file-watcher"                           % "0.1.0"
  lazy val pdfBox                   = "org.apache.pdfbox"   % "pdfbox"                                 % "3.0.0"
  lazy val scalaTest                = "org.scalatest"      %% "scalatest"                              % "3.3.0-SNAP4"
}