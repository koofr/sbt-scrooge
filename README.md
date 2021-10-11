# sbt-scrooge

sbt-scrooge is an sbt 0.12 plugin that adds support for using scrooge to
perform thrift code generation.

The plugin is forked from Twitter's [sbt-scrooge](https://github.com/twitter/sbt-scrooge) repo that is not maintained anymore.

## How it works

The plugin registers itself as a source generator for the compile phase.

It fetches scrooge from Ivy2 repository, caches it in your
project, and runs it against your thrift folder (usually `src/main/thrift`).
The generated code folder (usually `target/src_managed`) is then added to your
compile path.

## Using it

Add following to your `project/plugins.sbt` file:

    resolvers += Resolver.url("Koofr repo", url("http://koofr.github.com/repo/releases/"))(Resolver.ivyStylePatterns)

    addSbtPlugin("net.koofr" % "sbt-scrooge" % "3.0.45")

And `build.sbt`:

    import net.koofr.sbt._

    CompileThriftScrooge.newSettings

    name := "yourproject"

    organization := "com.example"

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

## Configuration

A full list of settings is in the (only) source file. Here are the ones you're
most likely to want to edit:

- `scroogeVersion: String`

  to use a different version of scrooge than the current default

- `scroogeCacheFolder: File`

  to unpack the downloaded scrooge release into a different folder

- `scroogeBuildOptions: Seq[String]`

  list of command-line arguments to pass to scrooge (default:
  `Seq("--finagle", "--ostrich", "--verbose")`)

- `scroogeThriftIncludeFolders: Seq[File]`

  list of folders to search when processing "include" directives (default: none)

- `scroogeThriftSourceFolder: File`

  where to find thrift files to compile (default: `src/main/thrift/`)

- `scroogeThriftOutputFolder: File`

  where to put the generated scala files (default: `target/<scala-ver>/src_managed`)


# Notes for helping work on sbt-scrooge

## Building

To build the plugin locally and publish it to your local filesystem:

    $ sbt publish-local

## Testing

To test the plugin run `scripted` command in sbt:

    $ sbt scripted
