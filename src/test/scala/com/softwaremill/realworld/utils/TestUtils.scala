package com.softwaremill.realworld.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.utils.DbData.exampleUser1
import com.softwaremill.realworld.{CustomDecodeFailureHandler, DefectHandler}
import io.getquill.*
import io.getquill.jdbczio.*
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.{RIO, Random, Scope, ZIO, ZLayer}

import java.nio.file.{Files, Path, Paths}
import java.sql.{Connection, Statement}
import java.util.UUID
import javax.sql.DataSource
import scala.io.Source

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

  type TestDbLayer = DbConfig with DataSource with DbMigrator with Quill.Sqlite[SnakeCase]

  def getValidAuthorizationHeader(email: String = exampleUser1.email): RIO[AuthService, Map[String, String]] =
    for {
      authService <- ZIO.service[AuthService]
      jwt <- authService.generateJwt(email)
    } yield Map("Authorization" -> s"Token $jwt")

  private def clearDb(cfg: DbConfig): RIO[Any, Unit] = for {
    dbPath <- ZIO.succeed(
      Paths.get(cfg.jdbcUrl.dropWhile(_ != '/'))
    )
    _ <- ZIO.attemptBlocking(
      Files.deleteIfExists(dbPath)
    )
  } yield ()

  private val initializeDb: RIO[DbMigrator, Unit] = for {
    migrator <- ZIO.service[DbMigrator]
    _ <- migrator.migrate()
  } yield ()

  private val createTestDbConfig: ZIO[Any, Nothing, DbConfig] = for {
    uuid <- Random.RandomLive.nextUUID
  } yield DbConfig(s"jdbc:sqlite:/tmp/realworld-test-$uuid.sqlite")

  private val testDbConfigLive: ZLayer[Any, Nothing, DbConfig] =
    ZLayer.scoped {
      ZIO.acquireRelease(acquire = createTestDbConfig)(release = config => clearDb(config).orDie)
    }

  val testDbLayer: ZLayer[Any, Nothing, TestDbLayer] =
    testDbConfigLive >+> Db.dataSourceLive >+> Db.quillLive >+> DbMigrator.live

  val testDbLayerWithEmptyDb: ZLayer[Any, Nothing, TestDbLayer] =
    testDbLayer >+> ZLayer.fromZIO(initializeDb.orDie)
