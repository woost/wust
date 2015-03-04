name := """wust"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "angularjs" % "1.3.14",
  "org.webjars" % "angular-ui-router" % "0.2.13",
  "org.webjars" % "angular-ui-bootstrap" % "0.12.1",
  "org.webjars" % "font-awesome" % "4.3.0-1",
  "org.webjars" % "bootstrap" % "3.3.2-1",
  "org.webjars" % "spin-js" % "2.0.0-1",
  "org.webjars" % "visjs" % "3.10.0",
  "org.webjars" % "toastr" % "2.1.0",
  "com.github.renesca" %% "renesca" % "0.1.2"
)

// javascript file groups
pipelineStages in Assets := Seq(concat)

Concat.parentDir := "public/main/javascripts"

val jsAssetsDirectory = baseDirectory(_ / "app/assets/javascripts")

Concat.groups := Seq(
  // http://www.scala-sbt.org/0.12.1/docs/Detailed-Topics/Paths.html
  "wust.js" -> group(
    (jsAssetsDirectory.value ** "*.js") ---
      (jsAssetsDirectory.value / "module.js")
  )
)

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Yinline", "-Yinline-warnings",
  "-language:_"
  //,"-Xdisable-assertions", "-optimize"
)
