import sbt.Project.projectToRef

name := "wust"

val scalaV = "2.11.6"

val paradiseVersion = "2.1.0-M5"

// TODO: use this syntax to share settings
// lazy val wust: Project = Project( "wust", file("."),
//   settings = sharedsettings
//   ) dependsOn(macros)

lazy val wust = (project in file(".")).settings(
  scalaVersion := scalaV,
  libraryDependencies ++= Seq(
    cache,
    ws,
    // database
    "com.github.renesca" %% "renesca" % "0.1.3",
    // scalajs
    "com.vmunier" %% "play-scalajs-scripts" % "0.1.0",
    // "org.webjars" %% "webjars-play" % "2.3.0-2",
    // angular
    "org.webjars" % "angularjs" % "1.3.15",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "org.webjars" % "angular-ui-bootstrap" % "0.12.1",
    "org.webjars" % "angular-dragdrop" % "1.0.8",
    // basic js libraries
    "org.webjars" % "lodash" % "3.3.1",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    "org.webjars" % "bootstrap" % "3.3.4",
    "org.webjars" % "d3js" % "3.5.3",
    "org.webjars" % "spin-js" % "2.0.0-1", // really?
    "org.webjars" % "animate.css" % "3.2.5", // do we still need this with compass animations?
    // atmosphere
    "org.atmosphere" % "atmosphere-play" % "2.1.1",
    "org.webjars" % "atmosphere-javascript" % "2.2.8",
    "javax.servlet" % "javax.servlet-api" % "3.1.0" withSources()
  ),
  // ecmascript 6
  TraceurKeys.sourceFileNames := Seq("javascripts/module.js", "javascripts/routes.js", "javascripts/*/**/*.js"),
  // use compass with sbt-sass
  sassOptions in Assets ++= Seq("--compass", "-r", "compass"),
  // scalaJSProjects := Seq(scalajs),
  scalacOptions ++= scalacOpts, //++ scalacMacroOpts,
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
).
enablePlugins(PlayScala, SbtWeb).
dependsOn(macros)
// aggregate(projectToRef(scalajs)).

lazy val scalajs = (project in file("scalajs")).
settings(
  scalaVersion := scalaV,
  persistLauncher := true, // launch main class on website load
  persistLauncher in Test := false,
  scalacOptions ++= scalacOpts
).enablePlugins(ScalaJSPlugin, ScalaJSPlay)

lazy val macros = (project in file("macros")).
settings(
  scalaVersion := scalaV,
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaV,
  scalacOptions ++= scalacOpts
)

val scalacOpts = Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Yinline", "-Yinline-warnings",
    "-language:_"
    //,"-Xdisable-assertions", "-optimize"
  )

val scalacMacroOpts = Seq(
    "-Ymacro-debug-lite",
    "-Yshow-trees-stringified"
)
