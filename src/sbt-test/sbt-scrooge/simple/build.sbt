import net.koofr.sbt._

CompileThriftScrooge.newSettings

name := "sbt-scrooge-test"

organization := "net.koofr"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.thrift" % "libthrift" % "0.8.0" intransitive,
  "com.twitter" %% "finagle-core" % "6.2.0",
  "com.twitter" %% "finagle-thrift" % "6.2.0",
  "com.twitter" %% "finagle-ostrich4" % "6.2.0",
  "com.twitter" %% "finagle-redis" % "6.2.0",
  "com.twitter" % "scrooge-runtime" % "3.0.43"
)

CompileThriftScrooge.scroogeBuildOptions := List("--finagle")
