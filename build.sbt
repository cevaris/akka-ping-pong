name := "Ping Pong"

version := "1.0"

scalaVersion := "2.11.5"

resolvers ++= Seq(
  "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.1.0",
  "org.slf4j"                  % "slf4j-api"       % "1.7.10",
  "ch.qos.logback"             % "logback-classic" % "1.1.2",
  "com.typesafe.akka"          %% "akka-actor"     % "2.3.10"
)
