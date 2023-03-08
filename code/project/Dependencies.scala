import sbt._

object Dependencies {
  lazy val AkkaVersion  = "2.7.0"
  lazy val POIVersion   = "5.2.2"

  lazy val akkaActor                = "com.typesafe.akka"       %% "akka-actor-typed"           % AkkaVersion
  lazy val akkaPersistence          = "com.typesafe.akka"       %% "akka-persistence-typed"     % AkkaVersion
  lazy val akkaPersistenceJDBC      = "com.lightbend.akka"      %% "akka-persistence-jdbc"      % "5.2.0"
  lazy val akkaSerializationJackson = "com.typesafe.akka"       %% "akka-serialization-jackson" % AkkaVersion
  lazy val alpakka                  = "com.lightbend.akka"      %% "akka-stream-alpakka-file"   % "5.0.0"
  lazy val apachePOI                = "org.apache.poi"          % "poi"                         % POIVersion
  lazy val apachePOIOOXML           = "org.apache.poi"          % "poi-ooxml"                   % POIVersion
  lazy val cats                     = "org.typelevel"           %% "cats-core"                  % "2.9.0"
  lazy val jna                      = "net.java.dev.jna"        % "jna-platform"                % "5.13.0"
  //lazy val jna                      = "net.java.dev.jna" % "platform" % "3.5.2" //"net.java.dev.jna" % "jna" % "5.12.1"
  lazy val logbackClassic           = "ch.qos.logback"          %  "logback-classic"            % "1.4.5"
  lazy val osLib                    = "com.lihaoyi"             %% "os-lib"                     % "0.9.0"
  lazy val pdfBox                   = "org.apache.pdfbox"       % "pdfbox"                      % "3.0.0-alpha2"
  lazy val postgreSQL               = "org.postgresql"          %  "postgresql"                 % "42.5.4"
  lazy val scalaParserCombinators   = "org.scala-lang.modules"  %% "scala-parser-combinators"   % "1.1.2"
  lazy val scallop                  = "org.rogach"              %% "scallop"                    % "4.1.0"
  lazy val scalaTest                = "org.scalatest"           %% "scalatest"                  % "3.2.15"
}