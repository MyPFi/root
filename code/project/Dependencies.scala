import sbt.*

object Dependencies {
  lazy val AkkaVersion                = "2.7.0"
  lazy val AkkaHttpVersion            = "10.5.0"
  lazy val AkkaManagementVersion      = "1.2.0"
  lazy val AkkaProjectionVersion      = "1.3.0"
  lazy val ScalikeJdbcVersion         = "3.5.0"

  lazy val akkaActor                      = "com.typesafe.akka"             %% "akka-actor-typed"                   % AkkaVersion
  lazy val akkaDiscovery                  = "com.typesafe.akka"             %% "akka-discovery"                     % AkkaVersion
  lazy val akkaDiscoveryKubernetesAPI     = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"      % AkkaManagementVersion
  lazy val akkaCluster                    = "com.typesafe.akka"             %% "akka-cluster-typed"                 % AkkaVersion
  lazy val akkaClusterSharding            = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"        % AkkaVersion
  lazy val akkaHTTP                       = "com.typesafe.akka"             %% "akka-http"                          % AkkaHttpVersion
  lazy val akkaHTTPSprayJSON              = "com.typesafe.akka"             %% "akka-http-spray-json"               % AkkaHttpVersion
  lazy val akkaManagement                 = "com.lightbend.akka.management" %% "akka-management"                    % AkkaManagementVersion
  lazy val akkaManagementClusterBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap"  % AkkaManagementVersion
  lazy val akkaManagementClusterHTTP      = "com.lightbend.akka.management" %% "akka-management-cluster-http"       % AkkaManagementVersion
  lazy val akkaPersistence                = "com.typesafe.akka"             %% "akka-persistence-typed"             % AkkaVersion
  lazy val akkaPersistenceJDBC            = "com.lightbend.akka"            %% "akka-persistence-jdbc"              % "5.2.0"
  lazy val akkaPersistenceQuery           = "com.typesafe.akka"             %% "akka-persistence-query"             % AkkaVersion
  lazy val akkaProjectionEventSourced     = "com.lightbend.akka"            %% "akka-projection-eventsourced"       % AkkaProjectionVersion
  lazy val akkaProjectionJDBC             = "com.lightbend.akka"            %% "akka-projection-jdbc"               % AkkaProjectionVersion
  lazy val akkaSerializationJackson       = "com.typesafe.akka"             %% "akka-serialization-jackson"         % AkkaVersion
  lazy val akkaSLF4J                      = "com.typesafe.akka"             %% "akka-slf4j"                         % AkkaVersion
  lazy val akkaStream                     = "com.typesafe.akka"             %% "akka-stream"                        % AkkaVersion
  lazy val akkaStreamKafka                = "com.typesafe.akka"             %% "akka-stream-kafka"                  % "4.0.0"
  lazy val alpakka                        = "com.lightbend.akka"            %% "akka-stream-alpakka-file"           % "5.0.0"
  lazy val apachePOIOOXML                 = "org.apache.poi"                % "poi-ooxml"                           % "5.2.2"
  lazy val cats                           = "org.typelevel"                 %% "cats-core"                          % "2.9.0"
  lazy val jna                            = "net.java.dev.jna"              % "jna-platform"                        % "5.13.0"//"net.java.dev.jna" % "platform" % "3.5.2" //"net.java.dev.jna" % "jna" % "5.12.1"
  lazy val logbackClassic                 = "ch.qos.logback"                %  "logback-classic"                    % "1.4.5"
  lazy val osLib                          = "com.lihaoyi"                   %% "os-lib"                             % "0.9.0"
  lazy val pdfBox                         = "org.apache.pdfbox"             % "pdfbox"                              % "3.0.0-alpha2"
  lazy val postgreSQL                     = "org.postgresql"                %  "postgresql"                         % "42.5.4"
  lazy val scalaParserCombinators         = "org.scala-lang.modules"        %% "scala-parser-combinators"           % "1.1.2"
  lazy val scalikeJDBC                    = "org.scalikejdbc"               %% "scalikejdbc"                        % ScalikeJdbcVersion
  lazy val scalikeJDBCConfig              = "org.scalikejdbc"               %% "scalikejdbc-config"                 % ScalikeJdbcVersion
  lazy val scallop                        = "org.rogach"                    %% "scallop"                            % "4.1.0"

  lazy val akkaActorTestKit               = "com.typesafe.akka"             %% "akka-actor-testkit-typed"           % AkkaVersion
  lazy val akkaPersistenceTestkit         = "com.typesafe.akka"             %% "akka-persistence-testkit"           % AkkaVersion
  lazy val akkaProjectionTestkit          = "com.lightbend.akka"            %% "akka-projection-testkit"            % AkkaProjectionVersion
  lazy val akkaStreamTestkit              = "com.typesafe.akka"             %% "akka-stream-testkit"                % AkkaVersion
  lazy val scalaTest                      = "org.scalatest"                 %% "scalatest"                          % "3.2.15"
}