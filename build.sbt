val emailValidatorVersion = "1.7"
val flywayVersion = "9.17.0"
val hikariVersion = "5.0.1"
val jwtVersion = "4.4.0"
val logbackVersion = "1.4.7"
val password4jVersion = "1.7.0"
val quillVersion = "4.6.0.1"
val sqliteVersion = "3.41.2.2"
val tapirVersion = "1.3.0"
val zioConfigVersion = "3.0.7"
val zioJsonVersion = "3.8.15"
val zioLoggingVersion = "2.1.12"
val zioTestVersion = "2.0.13"

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
  "com.softwaremill.sttp.client3" %% "zio-json" % zioJsonVersion % Test
)

val emailValidator = Seq("commons-validator" % "commons-validator" % emailValidatorVersion)

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "realworld-tapir-zio",
    version := "0.1.0-SNAPSHOT",
    organization := "com.softwaremill",
    scalaVersion := "3.2.2",
    Test / fork := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-explain",
      "-Wunused:all",
      "-Xmax-inlines",
      "64",
      "-Xfatal-warnings",
      "-Ycheck-all-patmat"
    ),
    libraryDependencies ++= tapir ++ config ++ security ++ db ++ tests ++ emailValidator,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)
