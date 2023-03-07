import sbt._

object Dependencies {
  lazy val akkaActor              = "com.typesafe.akka"       %% "akka-actor-typed"         % "2.7.0"
  lazy val alpakka                = "com.lightbend.akka"      %% "akka-stream-alpakka-file" % "5.0.0"
  lazy val apachePOI              = "org.apache.poi"          % "poi"                       % "5.2.2"
  lazy val apachePOIOOXML         = "org.apache.poi"          % "poi-ooxml"                 % "5.2.2"
  lazy val cats                   = "org.typelevel"           %% "cats-core"                % "2.9.0"
  lazy val logbackClassic         = "ch.qos.logback"          %  "logback-classic"          % "1.4.5"
  lazy val osLib                  = "com.lihaoyi"             %% "os-lib"                   % "0.9.0"
  lazy val pdfBox                 = "org.apache.pdfbox"       % "pdfbox"                    % "3.0.0-alpha2"
  lazy val scalaParserCombinators = "org.scala-lang.modules"  %% "scala-parser-combinators" % "1.1.2"
  lazy val scallop                = "org.rogach"              %% "scallop"                  % "4.1.0"
  lazy val scalaTest              = "org.scalatest"           %% "scalatest"                % "3.2.15"
}