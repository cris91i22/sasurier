resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.20")

addSbtPlugin("com.artima.supersafe" % "sbtplugin" % "1.1.3")

// Build plugin
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")