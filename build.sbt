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
  resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/",
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
    "org.webjars.bower" % "angular-ui-router" % "0.2.13",
    "org.webjars.bower" % "angular-bootstrap" % "0.12.1",
    "org.webjars.bower" % "angular-native-dragdrop" % "1.1.0",
    "org.webjars.bower" % "angular-restmod" % "1.1.8",
    // js auth
    "org.webjars.bower" % "angular-jwt" % "0.0.7",
    "org.webjars.bower" % "a0-angular-storage" % "0.0.9",
    // basic js libraries
    "org.webjars.bower" % "lodash" % "3.6.0",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    "org.webjars.bower" % "bootstrap" % "3.3.4",
    "org.webjars.bower" % "d3" % "3.5.5",
    "org.webjars.bower" % "humane-js" % "3.2.2",
    "org.webjars.bower" % "angular-xeditable" % "0.1.9",
    "org.webjars" % "animate.css" % "3.2.5",
    // authentication / authorization
    "com.mohiva" %% "play-silhouette" % "2.0",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
    // atmosphere
    "org.atmosphere" % "atmosphere-play" % "2.1.1" exclude("javax.servlet", "servlet-api"),
    "javax.servlet" % "javax.servlet-api" % "3.1.0", // to fix atmosphere-play 2.1.1 which wrongly depends on servlet-api 2.5 - https://github.com/Atmosphere/atmosphere-play/issues/30
    "org.webjars.bower" % "atmosphere" % "2.2.7"

  ),
  // ecmascript 6
  TraceurKeys.sourceFileNames := Seq("javascripts/*/module.js", "javascripts/*/**/*.js"),
  // use compass with sbt-sass
  sassOptions in Assets ++= Seq("--compass", "-r", "compass", "-r", "sass-globbing"),
  // scalaJSProjects := Seq(scalajs),
  scalacOptions ++= scalacOpts,
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
).
  enablePlugins(PlayScala, SbtWeb).
  dependsOn(schema).
  aggregate(macros, schema)

lazy val scalajs = (project in file("scalajs")).
  settings(
    scalaVersion := scalaV,
    persistLauncher := true, // launch main class on website load
    persistLauncher in Test := false,
    scalacOptions ++= scalacOpts
  ).enablePlugins(ScalaJSPlugin, ScalaJSPlay)

lazy val schema = (project in file("schema")).
  settings(
    scalaVersion := scalaV,
    scalacOptions ++= scalacOpts,
    // scalacOptions ++= scalacMacroOpts, // print code generated by macro
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
  ).
  dependsOn(macros)

lazy val macros = (project in file("macros")).
  settings(
    scalaVersion := scalaV,
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaV,
      "com.github.renesca" %% "renesca" % "0.1.3"
    ),
    scalacOptions ++= scalacOpts
  )

val scalacOpts = Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-Yinline", "-Yinline-warnings",
  "-language:_"
  //,"-Xdisable-assertions", "-optimize"
)

val scalacMacroOpts = Seq(
  "-Ymacro-debug-lite",
  "-Yshow-trees-stringified"
)
