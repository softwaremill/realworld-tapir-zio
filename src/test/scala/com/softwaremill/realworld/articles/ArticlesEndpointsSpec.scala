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
      suite("with auth data only")(
        test("return error on get") {
          assertZIO(
            for {
              articlesEndpoints <- ZIO.service[ArticlesEndpoints]
              endpoint = articlesEndpoints.get
              authHeader <- getValidAuthorizationHeader()
              response <- basicRequest
                .get(uri"http://test.com/api/articles/unknown-article")
                .headers(authHeader)
                .response(asJson[Article])
                .send(backendStub(endpoint))
              body = response.body
            } yield body
          )(isLeft(equalTo(HttpError("{\"error\":\"Article with slug unknown-article doesn't exist.\"}", sttp.model.StatusCode(404)))))
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithEmptyDb
      ),
      suite("with populated db")(
        test("get existing article") {
          assertZIO(
            for {
              articlesEndpoints <- ZIO.service[ArticlesEndpoints]
              endpoint = articlesEndpoints.get
              authHeader <- getValidAuthorizationHeader()
              response <- basicRequest
                .get(uri"http://test.com/api/articles/how-to-train-your-dragon-2")
                .headers(authHeader)
                .response(asJson[Article])
                .send(backendStub(endpoint))
              body = response.body
            } yield body
          )(
            isRight(
              equalTo(
                Article(
                  ArticleData(
                    "how-to-train-your-dragon-2",
                    "How to train your dragon 2",
                    "So toothless",
                    "Its a dragon",
                    List("dragons", "goats", "training"),
                    Instant.ofEpochMilli(1455765776637L),
                    Instant.ofEpochMilli(1455767315824L),
                    false,
                    1,
                    ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
                  )
                )
              )
            )
          )
        }
      ).provide(
        testArticlesLayer,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    ),
    suite("create article")(
      test("positive article creation") {
        for {
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.create
          authHeader <- getValidAuthorizationHeader()
          response <- basicRequest
            .post(uri"http://test.com/api/articles")
            .body(
              ArticleCreate(
                ArticleCreateData(
                  title = "How to train your dragon 2",
                  description = "So toothless",
                  body = "Its a dragon",
                  tagList = Some(List("drogon", "fly"))
                )
              )
            )
            .headers(authHeader)
            .response(asJson[Article])
            .send(backendStub(endpoint))
          body = response.body
        } yield assertTrue {
          // TODO there must be better way to implement this...
          import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}
          compare(
            body.toOption.get,
            Article(
              ArticleData(
                "how-to-train-your-dragon-2",
                "How to train your dragon 2",
                "So toothless",
                "Its a dragon",
                List("drogon", "fly"),
                null,
                null,
                false,
                0,
                ArticleAuthor("admin", Some("I dont work"), Some(""), following = false)
              )
            )
          ).isIdentical
        }
      },
      test("article creation - check conflict") {
        assertZIO(for {
          repo <- ZIO.service[ArticlesRepository]
          createData = ArticleCreateData(
            title = "Test slug",
            description = "So toothless",
            body = "Its a dragon",
            tagList = Some(List("drogon", "fly"))
          )
          _ <- repo.add(createData = createData, userId = 1)
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.create
          authHeader <- getValidAuthorizationHeader()
          response <- basicRequest
            .post(uri"http://test.com/api/articles")
            .body(
              ArticleCreate(createData)
            )
            .headers(authHeader)
            .response(asJson[Article])
            .send(backendStub(endpoint))
          body = response.body
        } yield body)(
          isLeft(
            equalTo(
              HttpError(
                "{\"error\":\"Article name already exists\"}",
                sttp.model.StatusCode.Conflict
              )
            )
          )
        )
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
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.update
          authHeader <- getValidAuthorizationHeader()
          response <- basicRequest
            .put(uri"http://test.com/api/articles/test-slug")
            .body(
              ArticleUpdate(
                ArticleUpdateData(
                  title = Option("Test slug 2"),
                  description = Option("updated description"),
                  body = Option("updated body")
                )
              )
            )
            .headers(authHeader)
            .response(asJson[Article])
            .send(backendStub(endpoint))
          body = response.body
        } yield assertTrue {
          // TODO there must be better way to implement this...
          import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}
          compare(
            body.toOption.get,
            Article(
              ArticleData(
                "test-slug-2",
                "Test slug 2",
                "updated description",
                "updated body",
                Nil,
                null,
                null,
                false,
                0,
                ArticleAuthor("admin", Some("I dont work"), Some(""), following = false)
              )
            )
          ).isIdentical
        }
      },
      test("article update - check conflict") {
        assertZIO(
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
            articlesEndpoints <- ZIO.service[ArticlesEndpoints]
            endpoint = articlesEndpoints.update
            authHeader <- getValidAuthorizationHeader()
            response <- basicRequest
              .put(uri"http://test.com/api/articles/test-slug")
              .body(
                ArticleUpdate(
                  ArticleUpdateData(
                    title = Option("Test slug 2"),
                    description = Option("updated description"),
                    body = Option("updated body")
                  )
                )
              )
              .headers(authHeader)
              .response(asJson[Article])
              .send(backendStub(endpoint))
            body = response.body
          } yield body
        )(
          isLeft(
            equalTo(
              HttpError(
                "{\"error\":\"Article name already exists\"}",
                sttp.model.StatusCode.Conflict
              )
            )
          )
        )
      }
    ).provide(
      testArticlesLayer,
      testDbLayerWithEmptyDb
    )
  )
