organization := "com.phasmidsoftware"

name := "Pairings"

version := "0.0.4"

scalaVersion := "2.13.6"

scalacOptions += "-deprecation"

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

lazy val scalaTestVersion = "3.2.9"

libraryDependencies ++= Seq(
    "com.phasmidsoftware" %%  "tableparser" % "1.0.14",
    "ch.qos.logback" % "logback-classic" % "1.2.6",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
