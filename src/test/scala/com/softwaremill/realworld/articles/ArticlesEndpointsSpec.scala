package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.ArticleTestSupport.*
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.profiles.{ProfilesRepository, ProfilesService}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.model.Uri
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.config.ReadError
import zio.test.Assertion.*
import zio.test.{Assertion, TestAspect, TestRandom, TestResult, ZIOSpecDefault, assertTrue, assertZIO}
import zio.{Cause, RIO, Random, ZIO, ZLayer}

import java.time.{Instant, ZonedDateTime}
import javax.sql.DataSource
import scala.language.postfixOps

object ArticlesEndpointsSpec extends ZIOSpecDefault:

  val base: ZLayer[Any, ReadError[String], AuthService & BaseEndpoints] =
    Configuration.live >+> AuthService.live >+> BaseEndpoints.live

  val repositories: ZLayer[TestDbLayer, Nothing, UsersRepository & ArticlesRepository & ProfilesRepository & TagsRepository] =
    UsersRepository.live ++ ArticlesRepository.live ++ ProfilesRepository.live >+> TagsRepository.live

  val testArticlesLayer: ZLayer[TestDbLayer, ReadError[String], AuthService & ArticlesRepository & ArticlesEndpoints] =
    (base ++ repositories) >+> ProfilesService.live >+> ArticlesService.live >+> ArticlesEndpoints.live

  def spec = suite("check articles endpoints")(
    suite("check articles list")(
      suite("with auth header")(
        suite("with auth data only")(
          test("return empty list") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- checkIfArticleListIsEmpty(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/articles")
            } yield result
          }
        ).provide(
          testArticlesLayer,
          testDbLayerWithEmptyDb
        ),
        suite("with populated db")(
          test("validation failed on filter") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- checkIfFilterErrorOccur(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?tag=invalid-tag"
              )
            } yield result
          },
          test("validation failed on pagination") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- checkIfPaginationErrorOccur(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
              )
            } yield result
          },
          test("check pagination") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- checkPagination(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?limit=1&offset=1"
              )
            } yield result
          },
          test("check filters") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- checkFilters(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
              )
            } yield result
          },
          test("list available articles") {
            for {
              authHeader <- getValidAuthorizationHeader()
              result <- listAvailableArticles(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles"
              )
            } yield result
          }
        ).provide(
          testArticlesLayer,
          testDbLayerWithFixture("fixtures/articles/basic-data.sql")
        )
      ),
      suite("with no header")(
        test("return empty list") {
          checkIfArticleListIsEmpty(authorizationHeaderOpt = None, uri = uri"http://test.com/api/articles")
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithEmptyDb
      ),
      suite("with populated db")(
        test("validation failed on filter") {
          checkIfFilterErrorOccur(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles?tag=invalid-tag"
          )
        },
        test("validation failed on pagination") {
          checkIfPaginationErrorOccur(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
          )
        },
        test("check pagination") {
          checkPagination(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles?limit=1&offset=1"
          )
        },
        test("check filters") {
          checkFilters(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
          )
        },
        test("list available articles") {
          listAvailableArticles(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles"
          )
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    ),
    suite("check feed article")(
      test("validation failed on pagination") {
        for {
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- checkIfPaginationErrorOccurInFeed(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/feed?limit=invalid-limit&offset=invalid-offset"
          )
        } yield result
      },
      test("check pagination") {
        for {
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- checkFeedPagination(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/feed?limit=1&offset=1"
          )
        } yield result
      },
      test("list available articles") {
        for {
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- listFeedAvailableArticles(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/feed"
          )
        } yield result
      }
    ).provide(
      testArticlesLayer,
      testDbLayerWithFixture("fixtures/articles/feed-data.sql")
    ),
    suite("check articles get")(
      suite("with empty db")(
        test("return error when requesting article doesn't exist") {
          for {
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfNonExistentArticleErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/unknown-article"
            )
          } yield result
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithEmptyDb
      ),
      suite("with populated db")(
        test("get existing article") {
          for {
            authHeader <- getValidAuthorizationHeader()
            result <- getAndCheckExistingArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2"
            )
          } yield result
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    ),
    suite("create article")(
      test("positive article creation") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- createAndCheckArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles",
            createData = ArticleCreateData(
              title = "How to train your dragon 2",
              description = "So toothless",
              body = "Its a dragon",
              tagList = Some(List("drogon", "fly"))
            )
          )
        } yield result
      },
      test("article creation - check conflict") {
        for {
          repo <- ZIO.service[ArticlesRepository]
          createData = ArticleCreateData(
            title = "Test slug",
            description = "So toothless",
            body = "Its a dragon",
            tagList = Some(List("drogon", "fly"))
          )
          _ <- repo.add(createData = createData, userId = 1)
          authHeader <- getValidAuthorizationHeader()
          result <- createAndCheckIfInvalidNameErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles",
            createData = createData
          )
        } yield result
      }
    ).provide(
      testArticlesLayer,
      testDbLayerWithEmptyDb
    ),
    suite("positive article deletion")(test("remove article and check if article list has two elements") {
      for {
        authHeader <- getValidAuthorizationHeader(email = "john@example.com")
        _ <- callDeleteArticle(
          authorizationHeader = authHeader,
          uri = uri"http://test.com/api/articles/how-to-train-your-dragon-3"
        )
        result <- checkArticlesListAfterDeletion(
          authorizationHeaderOpt = Some(authHeader),
          uri = uri"http://test.com/api/articles"
        )
      } yield result
    }).provide(
      testArticlesLayer,
      testDbLayerWithFixture("fixtures/articles/basic-data.sql")
    ),
    suite("update article")(
      test("positive article update") {
        for {
          repo <- ZIO.service[ArticlesRepository]
          _ <- repo.add(
            ArticleCreateData(
              title = "Test slug",
              description = "description",
              body = "body",
              tagList = None
            ),
            1
          )
          authHeader <- getValidAuthorizationHeader()
          result <- updateAndCheckArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/test-slug",
            updateData = ArticleUpdate(
              ArticleUpdateData(
                title = Option("Test slug 2"),
                description = Option("updated description"),
                body = Option("updated body")
              )
            )
          )
        } yield result
      },
      test("article update - check conflict") {
        for {
          repo <- ZIO.service[ArticlesRepository]
          _ <- repo.add(
            ArticleCreateData(
              title = "Test slug",
              description = "description",
              body = "body",
              tagList = None
            ),
            1
          )
          _ <- repo.add(
            ArticleCreateData(
              title = "Test slug 2",
              description = "description",
              body = "body",
              tagList = None
            ),
            1
          )
          authHeader <- getValidAuthorizationHeader()
          result <- updateAndCheckIfInvalidNameErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/test-slug",
            updateData = ArticleUpdate(
              ArticleUpdateData(
                title = Option("Test slug 2"),
                description = Option("updated description"),
                body = Option("updated body")
              )
            )
          )
        } yield result
      }
    ).provide(
      testArticlesLayer,
      testDbLayerWithEmptyDb
    )
  )
