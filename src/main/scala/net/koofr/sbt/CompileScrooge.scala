package net.koofr.sbt

import scala.collection.JavaConverters._

import sbt._
import Keys._

/**
 * An sbt 0.12 plugin for generating thrift with scrooge.
 */
object CompileThriftScrooge extends Plugin {
  // keys used to fetch scrooge:
  val scroogeVersion = SettingKey[String](
    "scrooge-version",
    "version of scrooge to download and use"
  )

  val scroogeName = SettingKey[String](
    "scrooge-name",
    "scrooge's version-qualified name ('scrooge-' + scrooge-version)"
  )

  val scroogeCacheFolder = SettingKey[File](
    "scrooge-cache-folder",
    "where to unpack the downloaded scrooge package"
  )

  val scroogeJar = SettingKey[File](
    "scrooge-jar",
    "the local scrooge jar file"
  )

  val scroogeFetch = TaskKey[File](
    "scrooge-fetch",
    "fetch the scrooge zip package and unpack it into scrooge-cache-folder"
  )

  // keys used for actual scrooge generation:
  val scroogeBuildOptions = SettingKey[Seq[String]](
    "scrooge-build-options",
    "command line args to pass to scrooge"
  )

  val scroogeThriftIncludeFolders = SettingKey[Seq[File]](
    "scrooge-thrift-include-folders",
    "folders to search for thrift 'include' directives"
  )

  val scroogeThriftNamespaceMap = SettingKey[Map[String, String]](
    "scrooge-thrift-namespace-map",
    "namespace rewriting, to support generation of java/finagle/scrooge into the same jar"
  )

  val scroogeThriftSourceFolder = SettingKey[File](
    "scrooge-thrift-source-folder",
    "directory containing thrift source files"
  )

  val scroogeThriftSources = SettingKey[Seq[File]](
    "scrooge-thrift-sources",
    "thrift source files to compile"
  )

  val scroogeThriftOutputFolder = SettingKey[File](
    "scrooge-thrift-output-folder",
    "output folder for generated scala files (defaults to sourceManaged)"
  )

  val scroogeGen = TaskKey[Seq[File]](
    "scrooge-gen",
    "generate scala code from thrift files using scrooge"
  )

  /**
   * these settings will go into both the compile and test configurations.
   * you can add them to other configurations by using inConfig(<config>)(genThriftSettings),
   * e.g. inConfig(Assembly)(genThriftSettings)
   */
  val genThriftSettings: Seq[Setting[_]] = Seq(
    scroogeThriftSourceFolder <<= (sourceDirectory) { _ / "thrift" },
    scroogeThriftSources <<= (scroogeThriftSourceFolder) { srcDir => (srcDir ** "*.thrift").get },
    scroogeThriftOutputFolder <<= (sourceManaged) { _ / "scala" },
    scroogeThriftIncludeFolders := Seq(),
    scroogeThriftNamespaceMap := Map(),

    // actually run scrooge
    scroogeGen <<= (
      streams,
      cacheDirectory,
      scroogeThriftSources,
      scroogeThriftOutputFolder,
      scroogeFetch,
      scroogeBuildOptions,
      scroogeThriftIncludeFolders,
      scroogeThriftNamespaceMap
    ) map { (out, cache, sources, outputDir, jar, opts, inc, ns) =>
      outputDir.mkdirs()

      // for some reason, sbt sometimes calls us multiple times, often with no source files.
      if (!sources.isEmpty) {
        val allSourceDeps = sources ++ inc.foldLeft(Seq[File]()) { (files, dir) =>
          files ++ (dir ** "*.thrift").get
        }

        val cacheFile = cache / "sbt-scrooge"
        val currentInfos = allSourceDeps.map(f => f.getAbsoluteFile() -> FileInfo.lastModified(f)).toMap

        val (previousRelation, previousInfo) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

        if (previousInfo != currentInfos) {
          out.log.info("Generating scrooge thrift for %s ...".format(sources.mkString(", ")))

          val sourcePaths = sources.mkString(" ")

          val namespaceMappings = ns.map { case (k, v) =>
            "-n " + k + "=" + v
          }.mkString(" ")

          val thriftIncludes = inc.map { folder =>
            "-i " + folder.getAbsolutePath
          }.mkString(" ")

          val cmd = "java -jar %s %s %s %s -d %s -s %s".format(
            jar, opts.mkString(" "), thriftIncludes, namespaceMappings,
            outputDir.getAbsolutePath, sources.mkString(" "))

          out.log.debug(cmd)

          cmd ! out.log

          Sync.writeInfo(cacheFile,
            Relation.empty[File, File],
            currentInfos)(FileInfo.lastModified.format)
        }
      }
      (outputDir ** "*.scala").get.toSeq
    },
    sourceGenerators <+= scroogeGen
  )

  val newSettings = Seq(
    scroogeVersion := "3.0.43",
    scroogeBuildOptions := Seq("--finagle", "--ostrich", "--verbose"),
    scroogeName <<= (scroogeVersion) { ver => "scrooge-generator-%s".format(ver) },
    scroogeCacheFolder <<= (baseDirectory) { (base) =>
      base / "project" / "target"
    },
    scroogeJar <<= (scroogeCacheFolder, scroogeName) { (folder, name) =>
      folder / (name + ".jar")
    },

    scroogeFetch <<= (
      streams,
      scroogeCacheFolder,
      scroogeJar,
      scroogeVersion
    ) map { (out, cacheFolder, jar, ver) =>
      if (!jar.exists) {
        out.log.info("Fetching scrooge " + ver + " ...")

        val environment = System.getenv().asScala
        val homeRepo = environment.get("SBT_PROXY_REPO") getOrElse "http://koofr.github.com/repo/maven"
        val localRepo = System.getProperty("user.home") + "/.m2/repository/"
        val jarPath = "/com/twitter/scrooge-generator/" + ver + "/scrooge-generator-" + ver + "-jar-with-dependencies.jar"
        val fetchUrl = if (new File(localRepo + jarPath).exists) {
          "file:" + localRepo + jarPath
        } else {
          homeRepo + jarPath
        }
        out.log.info("Fetching from: " + fetchUrl)

        cacheFolder.asFile.mkdirs()
        IO.download(new URL(fetchUrl), jar)

        if (jar.exists) {
          jar
        } else {
          error("failed to fetch and unpack %s".format(fetchUrl))
        }
      } else {
        jar
      }
    }
  ) ++ inConfig(Test)(genThriftSettings) ++ inConfig(Compile)(genThriftSettings)
}
