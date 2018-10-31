lazy val commonSettings = Seq(
  name := "sasurier",
  version := "0.1-SNAPSHOT",
  organization := "com.sasurier",
  version := sys.props.getOrElse("build.number", default = "SNAPSHOT"),
  scalaVersion := "2.12.2",
  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
  buildInfoPackage := "com.sasurier.app.build",
  buildInfoUsePackageAsPath := true,
  scalacOptions ++= Seq("-feature", "-deprecation"),
  resolvers ++= Seq(
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
    "Artima Maven Repository" at "http://repo.artima.com/releases",
    // Workaround so the project can be built directly from sbt on the build server
    Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
  ),
  libraryDependencies ++= Seq(
    evolutions, jdbc , ehcache , ws , specs2 % Test , guice,
    "com.typesafe.play" %% "play-json" % "2.6.10",
    "be.objectify" %% "deadbolt-scala" % "2.6.0",
    "org.postgresql" % "postgresql" % "42.2.5",
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "com.github.tminglei" %% "slick-pg" % "0.16.3",
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "io.paradoxical" %% "atmos" % "2.2"
  ),
  libraryDependencies += filters,
  fork in Test := true,
  parallelExecution in Test := false,
  javaOptions in Test += "-Dconfig.file=test/conf/application.test.conf"
)


lazy val `sasurier` = (project in file(".")).settings(commonSettings).enablePlugins(PlayScala, BuildInfoPlugin)