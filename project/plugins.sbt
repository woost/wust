resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)


  // The Play plugin
  addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")


  // Scala.js
  addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.2.3")

  addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.0")


  // web plugins
  addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")

  addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")

  addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

  addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")


  // use sass preprocessor for css
  addSbtPlugin("default" % "sbt-sass" % "0.1.9")


  // concatenate javascript files
  addSbtPlugin("net.ground5hark.sbt" % "sbt-concat" % "0.1.8")
