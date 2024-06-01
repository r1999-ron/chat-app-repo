name := """scala-chat-application"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.14"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  specs2 % Test,
  "org.playframework" %% "play-slick"            % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "mysql" % "mysql-connector-java" % "8.0.26"
)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test

resolvers += "Akka library repository" at "https://repo.akka.io/maven"

lazy val akkaVersion = sys.props.getOrElse("akka.version", "2.9.3")

fork := true // Run in a separate JVM

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.17"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.0"

libraryDependencies ++= Seq(
  // Akka dependencies
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.13",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,

  // Akka Stream Kafka
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "6.0.0",



  // Kafka client
  "org.apache.kafka" %% "kafka" % "3.7.0"
)
