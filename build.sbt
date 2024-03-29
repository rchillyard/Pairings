organization := "com.phasmidsoftware"

name := "Pairings"

version := "0.0.1"

scalaVersion := "2.13.3"

scalacOptions += "-deprecation"

resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"

lazy val scalaTestVersion = "3.1.1"

libraryDependencies ++= Seq(
    "com.phasmidsoftware" %%  "tableparser" % "1.0.13",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)
