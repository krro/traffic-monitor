name := "monitor"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "commons-codec" % "commons-codec" % "1.11"

libraryDependencies += "com.jayway.jsonpath" % "json-path" % "2.4.0"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.11"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.19"

libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
