resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")


// Scala.js
addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.2.7")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.4")


// web plugins
// https://www.mschweighauser.com/playframework-asset-pipeline/
// addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

// libraryDependencies += "org.webjars" % "jshint-node" % "2.6.3-2" // override sbt-jshint dependency until it contains the new version

// versioned assets
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

// compression
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

// addSbtPlugin("net.ground5hark.sbt" % "sbt-css-compress" % "0.1.3")

// addSbtPlugin("net.ground5hark.sbt" % "sbt-closure" % "0.1.3")

// ecmascript 6
// addSbtPlugin("com.typesafe.sbt" % "sbt-traceur" % "1.0.0")

// use sass preprocessor for css
// addSbtPlugin("default" % "sbt-sass" % "0.1.9")


// deploy to heroku from sbt
addSbtPlugin("com.heroku" % "sbt-heroku" % "0.4.3")
