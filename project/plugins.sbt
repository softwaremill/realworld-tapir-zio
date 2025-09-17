val sbtSoftwareMillVersion = "2.1.0"
val scalaFmtVersion = "2.5.1"
val sbtRevolverVersion = "0.10.0"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % scalaFmtVersion)
addSbtPlugin("com.softwaremill.sbt-softwaremill" % "sbt-softwaremill-common" % sbtSoftwareMillVersion)
addSbtPlugin("io.spray" % "sbt-revolver" % sbtRevolverVersion)
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.17.4")
