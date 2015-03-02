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

Concat.groups := Seq(
  "controllers.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "controllers") * "*.js"),
  "services.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "services") * "*.js"),
  "directives.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "directives") * "*.js"),
  "filters.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "filters") * "*.js")
)

Concat.parentDir := "public/main/javascripts"
