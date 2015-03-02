name := """wust"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.5"

pipelineStages := Seq(concat)

// javascript file groups
Concat.groups := Seq(
  "public/main/javascripts/controllers.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "controllers") * "*.js"),
  "public/main/javascripts/services.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "services") * "*.js"),
  "public/main/javascripts/directives.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "directives") * "*.js"),
  "public/main/javascripts/filters.js" -> group((baseDirectory.value / "app" / "assets"/ "javascripts" / "filters") * "*.js")
)

// settings for sbt-bower
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
