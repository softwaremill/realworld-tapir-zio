val currentScalaVersion = "3.7.2"
val emailValidatorVersion = "1.10.0"
val flywayVersion = "11.11.2"
val hikariVersion = "7.0.2"
val jwtVersion = "4.5.0"
val logbackVersion = "1.5.18"
val password4jVersion = "1.8.4"
val quillVersion = "4.8.6"
val sqliteVersion = "3.50.3.0"
val tapirVersion = "1.11.43"
val zioConfigVersion = "4.0.5"
val sttpZioJsonVersion = "3.11.0"
val zioLoggingVersion = "2.5.1"
val zioTestVersion = "2.1.20"
val zioMetrics = "2.5.0"

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

val monitoring = Seq(
  "dev.zio" %% "zio-metrics-connectors" % zioMetrics,
  "dev.zio" %% "zio-metrics-connectors-prometheus" % zioMetrics,
  "com.softwaremill.sttp.tapir" %% "tapir-zio-metrics" % tapirVersion
)

val emailValidator = Seq("commons-validator" % "commons-validator" % emailValidatorVersion)

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "realworld-tapir-zio",
    version := "0.1.0-SNAPSHOT",
    organization := "com.softwaremill",
    scalaVersion := currentScalaVersion,
    run / fork := true,
    Test / fork := true,
    scalacOptions ++= Seq(
      "-Xmax-inlines",
      "64"
    ),
    libraryDependencies ++= tapir ++ config ++ security ++ db ++ tests ++ emailValidator ++ monitoring,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
)

lazy val simulation = (project in file("simulation"))
  .enablePlugins(GatlingPlugin)
  .settings(inConfig(Gatling)(Defaults.testSettings))
  .settings(
    Seq(
      name := "realworld-tapir-zio-simulation",
      version := "0.1.0-SNAPSHOT",
      organization := "com.softwaremill",
      scalaVersion := currentScalaVersion,
      Test / fork := true,
      scalacOptions ++= Seq(
        "-Xmax-inlines",
        "64"
      ),
      libraryDependencies ++= Seq(
        "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.14.3",
        "io.gatling" % "gatling-test-framework" % "3.14.3",
        "net.datafaker" % "datafaker" % "2.4.4"
      )
    )
  )
