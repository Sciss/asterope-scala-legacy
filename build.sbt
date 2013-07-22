name := "Asterope"

version := "0.1.0-SNAPSHOT"

// organization := "de.sciss"

description := "Asterope astronomical program"

scalaVersion := "2.10.2"

// libraryDependencies in ThisBuild ++= Seq(
//   "de.sciss" %% "serial" % "1.0.+",
//   "org.scalatest" %% "scalatest" % "1.9.1" % "test"
// )

retrieveManaged := true

// mainClass in run := Some("org.asterope.gui.Main")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

