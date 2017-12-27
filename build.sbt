name := "discrepancy-vertica-aerospike"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.jsuereth" %% "scala-arm" % "2.0",

  "org.slf4j" % "log4j-over-slf4j" % "1.7.25",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",

  "com.vertica" % "vertica-jdbc" % "7.2.3-14",
  "com.aerospike" % "aerospike-client" % "3.3.4",

  "com.typesafe.akka" %% "akka-stream" % "2.5.6",
  "com.turbolent" %% "xdotai-diff" % "1.3.0",

  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.6" % Test
)

resolvers ++= Seq("turbolent" at "https://raw.githubusercontent.com/turbolent/mvn-repo/master/")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerBaseImage := "java:openjdk-8-jre"
dockerRepository := Some("http://docker.adform.com/")