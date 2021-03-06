name := "wust"

val scalaV = "2.11.8"

val paradiseVersion = "2.1.0"

// TODO: use this syntax to share settings
// lazy val wust: Project = Project( "wust", file("."),
//   settings = sharedsettings
//   ) dependsOn(macros)

lazy val wust = (project in file(".")).settings(
  scalaVersion := scalaV,
  resolvers += Resolver.jcenterRepo,
  resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releasesy",
  libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    // scalajs
    "com.vmunier" %% "play-scalajs-scripts" % "0.2.2",
    // in case we need to export shared code to js:
    // https://stackoverflow.com/questions/28413266/how-to-export-properties-of-shared-case-classes
    // authentication / authorization
    "com.mohiva" %% "play-silhouette" % "2.0.2" excludeAll (ExclusionRule(organization = "com.typesafe.play")),
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
    // database
    "commons-codec" % "commons-codec" % "1.10" // for base64 encoding of uuids
  ),
  // addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
  scalaJSProjects := Seq(scalajs),
  pipelineStages := Seq(scalaJSProd, digest, gzip),
  excludeFilter in gzip := (excludeFilter in gzip).value || new SimpleFileFilter(file => new File(file.getAbsolutePath + ".gz").exists), // do not compress assets for which a gzipped version already exists
  scalacOptions ++= scalacOpts,
  // disable gerenation of scaladoc
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false
).
  enablePlugins(PlayScala).
  dependsOn(schema, scalajsSharedJvm).
  aggregate(schema, scalajs, scalajsSharedJvm)

lazy val schema = (project in file("schema")).
  settings(
    scalaVersion := scalaV,
    resolvers += Resolver.jcenterRepo,
    resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releasesy",
    scalacOptions ++= scalacOpts,
    libraryDependencies ++= Seq(
      "com.github.renesca" %% "renesca" % "0.3.2-9",
      "com.github.renesca" %% "renesca-magic" % "0.3.4-1",
      // for external inheritance and default value code
      "com.mohiva" %% "play-silhouette" % "2.0.2" excludeAll (ExclusionRule(organization = "com.typesafe.play"))
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  ).
    dependsOn(scalajsSharedJs)

// ScalaJs
// https://github.com/vmunier/play-with-scalajs-example

import sbt.Project.projectToRef

lazy val scalajs = (project in file("scalajs")).settings(
  scalaVersion := scalaV,
  scalacOptions ++= scalacOpts,
  persistLauncher := false, // run Main automatically
  sourceMapsDirectories += scalajsSharedJs.base / "..",
  // we disable the scalajs optimizations,
  // because in production there is an error in the generated code
  // scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "utest" % "0.3.1"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  persistLauncher in Test := false,
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(scalajsSharedJs)

// http://www.scala-js.org/doc/sbt/cross-building.html
lazy val scalajsShared = (crossProject.crossType(CrossType.Pure) in file("scalajs-shared")).settings(
  scalaVersion := scalaV,
  scalacOptions ++= scalacOpts,
  libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "0.6.13"
).
  jsConfigure(_ enablePlugins ScalaJSPlay).
  jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val scalajsSharedJvm = scalajsShared.jvm
lazy val scalajsSharedJs = scalajsShared.js

lazy val seed = (project in file("seed")).settings(
  scalaVersion := scalaV,
  scalacOptions ++= scalacOpts,
  libraryDependencies ++= Seq(
    "com.github.seratch" %% "hackernews4s" % "0.5.0",
    // escape and unescape operations (html, json, css, ...)
    "org.unbescape" % "unbescape" % "1.1.1.RELEASE"
  ),
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false
).dependsOn(schema, wust)

// deploy to heroku
// herokuAppName in Compile := "wust"

val scalacOpts = Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-explaintypes",
  "-feature",
  //"-Yinline", "-Yinline-warnings",
  "-language:_",
  "-Xlint:_"
//,"-Xdisable-assertions", "-optimize"
)

val scalacMacroOpts = Seq(
  "-Ymacro-debug-lite",
  "-Yshow-trees-stringified"
)
