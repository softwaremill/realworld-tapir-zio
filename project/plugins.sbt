val sbtSoftwareMillVersion = "3.0.1"
val scalaFmtVersion = "2.6.2"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % scalaFmtVersion)
addSbtPlugin("com.softwaremill.sbt-softwaremill" % "sbt-softwaremill-common" % sbtSoftwareMillVersion)
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.19.2")
