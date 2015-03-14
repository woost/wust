import sbt.Project.projectToRef

name := "wust"

val jsAssetsDirectory = baseDirectory(_ / "app/assets/javascripts")

val scalaV = "2.11.6"

lazy val wust = (project in file(".")).settings(
  scalaVersion := scalaV,
  libraryDependencies ++= Seq(
    "org.webjars" %% "webjars-play" % "2.3.0-2",
    "com.vmunier" %% "play-scalajs-scripts" % "0.1.0",
    "org.webjars" % "angularjs" % "1.3.14",
    "org.webjars" % "angular-ui-router" % "0.2.13",
    "org.webjars" % "angular-ui-bootstrap" % "0.12.1",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    "org.webjars" % "bootstrap" % "3.3.2-2",
    "org.webjars" % "lodash" % "3.3.1",
    "org.webjars" % "spin-js" % "2.0.0-1",
    "org.webjars" % "visjs" % "3.11.0",
    "org.webjars" % "toastr" % "2.1.0",
    "org.webjars" % "animate.css" % "3.2.3",
    "org.webjars" % "angular-dragdrop" % "1.0.6-1",
    "com.github.renesca" %% "renesca" % "0.1.2"
  ),
// scalaJSProjects := Seq(scalajs),
// javascript file groups
pipelineStages := Seq(concat),
pipelineStages in Assets := Seq(concat),
Concat.parentDir := "public/main/javascripts",
Concat.groups := Seq(
  // http://www.scala-sbt.org/0.12.1/docs/Detailed-Topics/Paths.html
  "wust.js" -> group(
    (jsAssetsDirectory.value ** "*.js") ---
    (jsAssetsDirectory.value / "module.js")
  )
),
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



