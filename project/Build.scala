import sbt._
import Keys._

object SbtScroogePlugin extends Build {
  lazy val root = Project(
    id = "sbt-scrooge",
    base = file(".")
  ).settings(
    name := "sbt-scrooge",
    organization := "net.koofr",
    version := "3.0.44",
    sbtPlugin := true,

    resolvers += "Koofr Maven repo" at "http://koofr.github.com/repo/maven/"
  )
}
