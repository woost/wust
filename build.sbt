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
    // scalajs
    // "com.vmunier" %% "play-scalajs-scripts" % "0.2.0",
    // angular
    "org.webjars" % "angularjs" % "1.3.15",
    "org.webjars.bower" % "angular-ui-router" % "0.2.14",
    "org.webjars.bower" % "angular-bootstrap" % "0.13.0",
    "org.webjars.bower" % "angular-native-dragdrop" % "1.1.0",
    "org.webjars.bower" % "angular-restmod" % "1.1.8",
    "org.webjars.bower" % "angular-xeditable" % "0.1.9",
    // styles and fonts
    "org.webjars" % "animate.css" % "3.2.5",
    "org.webjars.bower" % "bootstrap-css-only" % "3.3.4",
    "org.webjars" % "font-awesome" % "4.3.0-1",
    // js auth
    "org.webjars.bower" % "angular-jwt" % "0.0.7",
    "org.webjars.bower" % "a0-angular-storage" % "0.0.9",
    // basic js libraries
    "org.webjars.bower" % "lodash" % "3.8.0",
    "org.webjars.bower" % "d3" % "3.5.5",
    "org.webjars.bower" % "humane-js" % "3.2.2",
    // authentication / authorization
    "com.mohiva" %% "play-silhouette" % "2.0",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
    // atmosphere
    "org.atmosphere" % "atmosphere-play" % "2.1.2",
    "org.webjars.bower" % "atmosphere" % "2.2.7",
    // database
    "com.github.renesca" %% "renesca" % "0.2.2",
    "com.github.renesca" %% "renesca-magic" % "0.1.2"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
  // ecmascript 6
  TraceurKeys.sourceFileNames := Seq("app/*/module.js", "app/*/*/**/*.js"),
  // use compass with sbt-sass
  sassOptions in Assets ++= Seq("--compass", "-r", "compass", "-r", "sass-globbing", "--update", "./app/assets:./target/web/public/main"),
  // scalaJSProjects := Seq(scalajs),
  scalacOptions ++= scalacOpts
).
  enablePlugins(PlayScala, SbtWeb)

// lazy val scalajs = (project in file("scalajs")).
//   settings(
//     scalaVersion := scalaV,
//     persistLauncher := true, // launch main class on website load
//     persistLauncher in Test := false,
//     scalacOptions ++= scalacOpts
//   ).enablePlugins(ScalaJSPlugin, ScalaJSPlay)

val scalacOpts = Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-Yinline", "-Yinline-warnings",
  "-language:_",
  "-Xlint:_"
  //,"-Xdisable-assertions", "-optimize"
)

val scalacMacroOpts = Seq(
  "-Ymacro-debug-lite",
  "-Yshow-trees-stringified"
)
