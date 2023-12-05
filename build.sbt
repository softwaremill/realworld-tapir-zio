val currentScalaVersion = "3.3.1"
val emailValidatorVersion = "1.7"
val flywayVersion = "10.1.0"
val hikariVersion = "5.1.0"
val jwtVersion = "4.4.0"
val logbackVersion = "1.4.14"
val password4jVersion = "1.7.3"
val quillVersion = "4.8.0"
val sqliteVersion = "3.42.0.1"
val tapirVersion = "1.9.3"
val zioConfigVersion = "4.0.0-RC16"
val sttpZioJsonVersion = "3.9.1"
val zioLoggingVersion = "2.1.16"
val zioTestVersion = "2.0.19"

val tapir = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
)

val config = Seq(
  "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
  "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
)

val security = Seq(
  "com.password4j" % "password4j" % password4jVersion,
  "com.auth0" % "java-jwt" % jwtVersion
)

val db = Seq(
  "org.xerial" % "sqlite-jdbc" % sqliteVersion,
  "org.flywaydb" % "flyway-core" % flywayVersion,
  "com.zaxxer" % "HikariCP" % hikariVersion,
  "io.getquill" %% "quill-jdbc-zio" % quillVersion
)

val tests = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
  "dev.zio" %% "zio-logging" % zioLoggingVersion,
  "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "dev.zio" %% "zio-test" % zioTestVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
  "com.softwaremill.sttp.client3" %% "zio-json" % sttpZioJsonVersion % Test
)

val emailValidator = Seq("commons-validator" % "commons-validator" % emailValidatorVersion)

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "realworld-tapir-zio",
    version := "0.1.0-SNAPSHOT",
    organization := "com.softwaremill",
    scalaVersion := currentScalaVersion,
    Test / fork := true,
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "64"
    ),
    libraryDependencies ++= tapir ++ config ++ security ++ db ++ tests ++ emailValidator,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)
