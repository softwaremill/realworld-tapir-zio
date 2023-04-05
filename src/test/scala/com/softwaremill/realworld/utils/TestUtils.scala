package com.softwaremill.realworld.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.utils.TestUtils.TestDbLayer
import com.softwaremill.realworld.{CustomDecodeFailureHandler, DefectHandler}
import io.getquill.{SnakeCase, SqliteZioJdbcContext}
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.TestRandom
import zio.{RIO, Random, Scope, ZIO, ZLayer}

import java.nio.file.{Files, Path, Paths}
import java.sql.{Connection, Statement}
import java.time.{Duration, Instant}
import java.util.UUID
import javax.sql.DataSource
import scala.io.Source
import scala.util.{Try, Using}

object TestUtils:

  def zioTapirStubInterpreter: TapirStubInterpreter[[_$1] =>> RIO[Any, _$1], Nothing, ZioHttpServerOptions[Any]] =
    TapirStubInterpreter(
      ZioHttpServerOptions.customiseInterceptors
        .exceptionHandler(new DefectHandler())
        .decodeFailureHandler(CustomDecodeFailureHandler.create()),
      SttpBackendStub(new RIOMonadError[Any])
    )

  def backendStub(endpoint: ZServerEndpoint[Any, Any]): SttpBackend[[_$1] =>> RIO[Any, _$1], Nothing] =
    zioTapirStubInterpreter
      .whenServerEndpoint(endpoint)
      .thenRunLogic()
      .backend()

  type TestDbLayer = DbConfig with DataSource with DbMigrator with SqliteZioJdbcContext[SnakeCase]

  def validAuthorizationHeader(email: String = "admin@example.com"): Map[String, String] = {
    // start TODO [This is workaround. Need to replace below with service's function call]
    val now: Instant = Instant.now()
    val Issuer = "SoftwareMill"
    val ClaimName = "userEmail"
    val YouShouldNotKeepSecretsHardcoded = "#>!IEd!G-L70@OTr$t8E[4.#[A;zo2@{"
    val algorithm: Algorithm = Algorithm.HMAC256(YouShouldNotKeepSecretsHardcoded)
    val jwt: String = JWT
      .create()
      .withIssuer(Issuer)
      .withClaim(ClaimName, email)
      .withIssuedAt(now)
      .withExpiresAt(now.plus(Duration.ofHours(1)))
      .withJWTId(UUID.randomUUID().toString)
      .sign(algorithm)
    // end TODO
    Map("Authorization" -> ("Token " + jwt))
  }

  private def loadFixture(fixturePath: String): RIO[DataSource & Scope, Unit] = for {
    _ <- ZIO.scope
    dataSource <- ZIO.service[DataSource]
    fixture <- ZIO.fromAutoCloseable(ZIO.attemptBlocking(Source.fromResource(fixturePath)))
    connection <- ZIO.fromAutoCloseable(ZIO.attempt(dataSource.getConnection))
    statement <- ZIO.fromAutoCloseable(ZIO.attempt(connection.createStatement()))
  } yield {
    val queries = fixture.mkString
      .split(";")
      .map(_.strip())
      .filter(_.nonEmpty)

    queries.foreach(statement.execute)
  }

  private def clearDb: RIO[DbConfig, Unit] = for {
    cfg <- ZIO.service[DbConfig]
    _ <- ZIO.attemptBlocking(Files.deleteIfExists(Paths.get(cfg.dbPath)))
  } yield ()

  private val initializeDb: RIO[DbMigrator, Unit] = for {
    migrator <- ZIO.service[DbMigrator]
    _ <- migrator.migrate()
  } yield ()

  private val withEmptyDb: RIO[DataSource with Scope with DbMigrator, Unit] = for {
    _ <- initializeDb
    _ <- loadFixture("fixtures/articles/admin.sql")
  } yield ()

  private def withFixture(fixturePath: String): RIO[DataSource with Scope with DbMigrator, Unit] = for {
    _ <- withEmptyDb
    _ <- loadFixture(fixturePath)
  } yield ()

  /*
  Clean up logic is packaged into a ZLayer.
  This ensures that *.sqlite file will always get deleted after the test.
  Clean up layer must be placed before the layer that provides a DataSource.
  Placing deleteDbLayer before Db.dataSourceLive guarantees that clean up will happen after DataSource is closed.
   */
  private val deleteDbLayer = {
    val release = (_: Unit) =>
      (for {
        _ <- ZIO.scope
        _ <- clearDb
      } yield ()).orDie

    ZLayer
      .service[DbConfig]
      .flatMap { _ =>
        ZLayer.scoped {
          ZIO.acquireRelease(ZIO.unit)(release)
        }
      }
  }

  private def createTestDbConfig(): ZIO[Random, Nothing, DbConfig] = for {
    _ <- TestRandom.setSeed(System.nanoTime())
    r <- ZIO.random
    uuid <- r.nextUUID
  } yield DbConfig(s"/tmp/realworld-test-$uuid.sqlite")

  private val testDbConfigLive: ZLayer[Any, Nothing, DbConfig] =
    ZLayer.fromZIO(createTestDbConfig().provide(ZLayer.fromZIO(ZIO.random)))

  val testDbLayer: ZLayer[Any, Nothing, TestDbLayer] =
    (testDbConfigLive >+> deleteDbLayer >+> Db.dataSourceLive >+> DbMigrator.live) ++ Db.quillLive

  val testDbLayerWithEmptyDb: ZLayer[Any, Nothing, TestDbLayer] =
    testDbLayer >+> ZLayer.scoped(withEmptyDb.orDie)

  def testDbLayerWithFixture(fixturePath: String): ZLayer[Any, Nothing, TestDbLayer] =
    testDbLayer >+> ZLayer.scoped(withFixture(fixturePath).orDie)
