import sbt.Project.projectToRef

name := "wust"

val scalaV = "2.11.6"

lazy val wust = (project in file(".")).settings(
  scalaVersion := scalaV,
  libraryDependencies ++= Seq(
    // database
    "com.github.renesca" %% "renesca" % "0.1.3",
    // scalajs
    "com.vmunier" %% "play-scalajs-scripts" % "0.1.0",
    // "org.webjars" %% "webjars-play" % "2.3.0-2",
    // angular
    "org.webjars" % "angularjs" % "1.3.14",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "org.webjars" % "angular-ui-bootstrap" % "0.12.1",
    "org.webjars" % "angular-dragdrop" % "1.0.6-1",
    // basic js libraries
    "org.webjars" % "lodash" % "3.3.1",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    "org.webjars" % "bootstrap" % "3.3.2-2",
    "org.webjars" % "d3js" % "3.5.3",
    "org.webjars" % "toastr" % "2.1.0",
    "org.webjars" % "spin-js" % "2.0.0-1", // really?
    "org.webjars" % "animate.css" % "3.2.3", // do we still need this with compass animations?
    // atmosphere
    "org.atmosphere" % "atmosphere-play" % "2.1.0",
    "org.webjars" % "atmosphere-javascript" % "2.2.3"
  ),
  // ecmascript 6
  TraceurKeys.sourceFileNames := Seq("javascripts/module.js", "javascripts/routes.js", "javascripts/*/*.js"),
  // use compass with sbt-sass
  sassOptions in Assets ++= Seq("--compass", "-r", "compass"),
  // scalaJSProjects := Seq(scalajs),
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Yinline", "-Yinline-warnings",
    "-language:_"
    //,"-Xdisable-assertions", "-optimize"
  )
).
enablePlugins(PlayScala, SbtWeb)
// aggregate(projectToRef(scalajs))

lazy val scalajs = (project in file("scalajs")).
settings(
  scalaVersion := scalaV,
  persistLauncher := true, // launch main class on website load
  persistLauncher in Test := false
).enablePlugins(ScalaJSPlugin, ScalaJSPlay)

