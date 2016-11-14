resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.sonatypeRepo("releases")

resolvers += Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")

// Scala.js
addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.2.7")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.10")

// versioned assets
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

// compression
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

// deploy to heroku from sbt
// addSbtPlugin("com.heroku" % "sbt-heroku" % "0.4.3")
