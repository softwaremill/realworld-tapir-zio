package com.softwaremill.realworld.utils

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{CustomDecodeFailureHandler, DefectHandler}
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.exampleUser1
import io.getquill.*
import io.getquill.jdbczio.*
import sttp.client3.SttpBackend
import sttp.client3.testing.SttpBackendStub
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.{RIO, Random, ZIO, ZLayer}

import java.nio.file.{Files, Paths}
import javax.sql.DataSource

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

  def getValidTokenAuthorizationHeader(email: String = exampleUser1.email): RIO[AuthService, Map[String, String]] =
    for {
      authService <- ZIO.service[AuthService]
      jwt <- authService.generateJwt(email)
    } yield Map("Authorization" -> s"Token $jwt")

  def getValidBearerAuthorizationHeader(email: String = exampleUser1.email): RIO[AuthService, Map[String, String]] =
    for {
      authService <- ZIO.service[AuthService]
      jwt <- authService.generateJwt(email)
    } yield Map("Authorization" -> s"Bearer $jwt")

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

  def findUserIdByEmail(userRepo: UsersRepository, email: String): ZIO[Any, Serializable, Int] =
    userRepo
      .findUserIdByEmail(email)
      .someOrFail(s"User with email $email doesn't exist.")

  def findArticleIdBySlug(articleRepo: ArticlesRepository, slug: String): ZIO[Any, Serializable, Int] =
    articleRepo
      .findArticleIdBySlug(slug)
      .someOrFail(s"Article $slug doesn't exist")
