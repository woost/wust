name := "wust"

val scalaV = "2.11.7"

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
    "org.webjars.bower" % "angular" % "1.4.2",
    "org.webjars.bower" % "angular-animate" % "1.4.2",
    "org.webjars.bower" % "angular-sanitize" % "1.4.2",
    "org.webjars.bower" % "angular-ui-router" % "0.2.15",
    "org.webjars.bower" % "angular-bootstrap" % "0.13.0",
    "org.webjars.bower" % "angular-native-dragdrop" % "1.1.0",
    "org.webjars.bower" % "angular-restmod" % "1.1.8",
    "org.webjars.npm" % "angular-ui-layout" % "1.0.5-requirejs",
    "org.webjars.bower" % "a0-angular-storage" % "0.0.11",
    "org.webjars.bower" % "ace-builds" % "1.1.9",
    "org.webjars.bower" % "angular-ui-ace" % "0.2.3",
    // styles and fonts
    "org.webjars.bower" % "animate.css" % "3.3.0",
    "org.webjars.bower" % "bootstrap-css-only" % "3.3.4",
    "org.webjars.bower" % "font-awesome" % "4.3.0",
    "org.webjars.bower" % "lodium" % "0.1.2",
    // js auth
    "org.webjars.bower" % "angular-jwt" % "0.0.9",
    // basic js libraries
    "org.webjars.bower" % "lodash" % "3.10.0",
    "org.webjars.bower" % "d3" % "3.5.6",
    "org.webjars.bower" % "humane-js" % "3.2.2",
    // markdown parser
    "org.webjars.bower" % "marked" % "0.3.3",
    // authentication / authorization
    "com.mohiva" %% "play-silhouette" % "2.0",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1",
    // atmosphere
    "org.atmosphere" % "atmosphere-play" % "2.1.2",
    "org.webjars.bower" % "atmosphere" % "2.2.7",
    // database
    "commons-codec" % "commons-codec" % "1.10" // for base64 encoding of uuids
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),
  // scalacOptions ++= scalacMacroOpts, // print code generated by macro
  // ecmascript 6
  TraceurKeys.sourceFileNames := Seq("app/module.js", "app/*/**/*.js"),
  // use compass with sbt-sass
  sassOptions in Assets ++= Seq("--compass", "-r", "compass", "-r", "sass-globbing", "--update", "./app/assets:./target/web/public/main"),
  // scalaJSProjects := Seq(scalajs),
  pipelineStages := Seq(/*closure, */ cssCompress, digest, gzip),
  excludeFilter in gzip := (excludeFilter in gzip).value || new SimpleFileFilter(file => new File(file.getAbsolutePath + ".gz").exists), // do not compress assets for which a gzipped version already exists
  // includeFilter in closure := (includeFilter in closure).value && new SimpleFileFilter(f => f.getName.contains("main.js") ),
  // Closure.flags := Seq("--language_in=ECMASCRIPT5"),
  includeFilter in cssCompress := (includeFilter in cssCompress).value && new SimpleFileFilter(f => f.getName.contains("app")),
  scalacOptions ++= scalacOpts
).
  enablePlugins(PlayScala, SbtWeb).
  dependsOn(schema).
  aggregate(schema)

lazy val schema = (project in file("schema")).
  settings(
    scalaVersion := scalaV,
    scalacOptions ++= scalacOpts,
    libraryDependencies ++= Seq(
      "com.github.renesca" %% "renesca" % "0.3.1",
      "com.github.renesca" %% "renesca-magic" % "0.3.1",
      // for external inheritance and default value code
      "com.mohiva" %% "play-silhouette" % "2.0"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
  )



// lazy val scalajs = (project in file("scalajs")).
//   settings(
//     scalaVersion := scalaV,
//     persistLauncher := true, // launch main class on website load
//     persistLauncher in Test := false,
//     scalacOptions ++= scalacOpts
//   ).enablePlugins(ScalaJSPlugin, ScalaJSPlay)

lazy val seed = (project in file("seed")).settings(
  scalaVersion := scalaV,
  scalacOptions ++= scalacOpts
).dependsOn(wust)

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
