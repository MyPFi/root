import sbt.*

object Dependencies {
  lazy val AkkaVersion                = "2.9.0-M2"

  lazy val akkaActor                      = "com.typesafe.akka"             %% "akka-actor-typed"                   % AkkaVersion
  lazy val akkaPersistence                = "com.typesafe.akka"             %% "akka-persistence-typed"             % AkkaVersion
  lazy val akkaPersistenceJDBC            = "com.lightbend.akka"            %% "akka-persistence-jdbc"              % "5.2.1"
  lazy val akkaSerializationJackson       = "com.typesafe.akka"             %% "akka-serialization-jackson"         % AkkaVersion
  lazy val alpakka                        = "com.lightbend.akka"            %% "akka-stream-alpakka-file"           % "6.0.2"
  lazy val apachePOIOOXML                 = "org.apache.poi"                % "poi-ooxml"                           % "5.2.4"
  lazy val cats                           = "org.typelevel"                 %% "cats-core"                          % "2.10.0"
  lazy val jna                            = "net.java.dev.jna"              % "jna-platform"                        % "5.13.0"
  lazy val logbackClassic                 = "ch.qos.logback"                %  "logback-classic"                    % "1.4.11"
  lazy val osLib                          = "com.lihaoyi"                   %% "os-lib"                             % "0.9.1"
  lazy val pdfBox                         = "org.apache.pdfbox"             % "pdfbox"                              % "3.0.0"
  lazy val postgreSQL                     = "org.postgresql"                %  "postgresql"                         % "42.6.0"
  lazy val scalaParserCombinators         = "org.scala-lang.modules"        %% "scala-parser-combinators"           % "2.3.0"
  lazy val scallop                        = "org.rogach"                    %% "scallop"                            % "5.0.0"
  lazy val scalaTest                      = "org.scalatest"                 %% "scalatest"                          % "3.3.0-SNAP4"
}