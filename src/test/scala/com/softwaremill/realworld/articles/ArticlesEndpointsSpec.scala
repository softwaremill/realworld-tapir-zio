package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.ArticlesSpecData.*
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.Exceptions.AlreadyInUse
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.profiles.{ProfilesRepository, ProfilesService}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.model.Uri
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.Assertion.*
import zio.test.{Assertion, TestAspect, TestRandom, TestResult, ZIOSpecDefault, assertTrue, assertZIO}
import zio.{Cause, RIO, Random, ZIO, ZLayer}

import java.time.{Instant, ZonedDateTime}
import javax.sql.DataSource
import scala.language.postfixOps

object ArticlesEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("check articles endpoints")(
    suite("check articles list")(
      suite("with auth header")(
        suite("with auth data only")(
          test("return empty list") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- checkIfArticleListIsEmpty(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/articles")
            } yield result
          }
        ).provide(
          Configuration.live,
          AuthService.live,
          UsersRepository.live,
          ArticlesRepository.live,
          ArticlesService.live,
          ArticlesEndpoints.live,
          BaseEndpoints.live,
          ProfilesRepository.live,
          ProfilesService.live,
          testDbLayerWithEmptyDb
        ),
        suite("with populated db")(
          test("validation failed on filter") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- checkIfFilterErrorOccur(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?tag=invalid-tag"
              )
            } yield result
          },
          test("validation failed on pagination") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- checkIfPaginationErrorOccur(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
              )
            } yield result
          },
          test("check pagination") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- checkPagination(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?limit=1&offset=1"
              )
            } yield result
          },
          test("check filters") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- checkFilters(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
              )
            } yield result
          },
          test("list available articles") {
            for {
              authHeader <- newValidAuthorizationHeader()
              result <- listAvailableArticles(
                authorizationHeaderOpt = Some(authHeader),
                uri = uri"http://test.com/api/articles"
              )
            } yield result
          }
        ).provide(
          Configuration.live,
          AuthService.live,
          UsersRepository.live,
          ArticlesRepository.live,
          ArticlesService.live,
          ArticlesEndpoints.live,
          BaseEndpoints.live,
          ProfilesRepository.live,
          ProfilesService.live,
          testDbLayerWithFixture("fixtures/articles/basic-data.sql")
        )
      ),
      suite("with no header")(
        test("return empty list") {
          checkIfArticleListIsEmpty(authorizationHeaderOpt = None, uri = uri"http://test.com/api/articles")
        }
      ).provide(
        Configuration.live,
        AuthService.live,
        UsersRepository.live,
        ArticlesRepository.live,
        ArticlesService.live,
        ArticlesEndpoints.live,
        BaseEndpoints.live,
        ProfilesRepository.live,
        ProfilesService.live,
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
        Configuration.live,
        AuthService.live,
        UsersRepository.live,
        ArticlesRepository.live,
        ArticlesService.live,
        ArticlesEndpoints.live,
        BaseEndpoints.live,
        ProfilesRepository.live,
        ProfilesService.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    ),
    suite("check articles get")(
      suite("with auth data only")(
        test("return error on get") {
          assertZIO(
            for {
              articlesEndpoints <- ZIO.service[ArticlesEndpoints]
              endpoint = articlesEndpoints.get
              authHeader <- newValidAuthorizationHeader()
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
        Configuration.live,
        AuthService.live,
        UsersRepository.live,
        ArticlesRepository.live,
        ArticlesService.live,
        ArticlesEndpoints.live,
        BaseEndpoints.live,
        ProfilesRepository.live,
        ProfilesService.live,
        testDbLayerWithEmptyDb
      ),
      suite("with populated db")(
        test("get existing article") {
          assertZIO(
            for {
              articlesEndpoints <- ZIO.service[ArticlesEndpoints]
              endpoint = articlesEndpoints.get
              authHeader <- newValidAuthorizationHeader()
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
        Configuration.live,
        AuthService.live,
        UsersRepository.live,
        ArticlesRepository.live,
        ArticlesService.live,
        ArticlesEndpoints.live,
        BaseEndpoints.live,
        ProfilesRepository.live,
        ProfilesService.live,
        testDbLayerWithFixture("fixtures/articles/basic-data.sql")
      )
    ),
    suite("create article")(
      test("positive article creation") {
        for {
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.create
          authHeader <- newValidAuthorizationHeader()
          response <- basicRequest
            .post(uri"http://test.com/api/articles")
            .body(
              ArticleCreate(
                ArticleCreateData(
                  title = "How to train your dragon 2",
                  description = "So toothless",
                  body = "Its a dragon",
                  tagList = List("drogon", "fly")
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
          _ <- repo.add(
            ArticleRow(
              slug = "test-slug",
              title = "title",
              description = "description",
              body = "body",
              createdAt = Instant.now,
              updatedAt = Instant.now,
              authorId = 1
            )
          )
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.create
          authHeader <- newValidAuthorizationHeader()
          response <- basicRequest
            .post(uri"http://test.com/api/articles")
            .body(
              ArticleCreate(
                ArticleCreateData(
                  title = "Test slug",
                  description = "So toothless",
                  body = "Its a dragon",
                  tagList = List("drogon", "fly")
                )
              )
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
      Configuration.live,
      AuthService.live,
      UsersRepository.live,
      ArticlesRepository.live,
      ArticlesService.live,
      ArticlesEndpoints.live,
      BaseEndpoints.live,
      ProfilesRepository.live,
      ProfilesService.live,
      testDbLayerWithEmptyDb
    ),
    suite("update article")(
      test("positive article update") {
        for {
          repo <- ZIO.service[ArticlesRepository]
          _ <- repo.add(
            ArticleRow(
              slug = "test-slug",
              title = "Test slug",
              description = "description",
              body = "body",
              createdAt = Instant.now,
              updatedAt = Instant.now,
              authorId = 1
            )
          )
          articlesEndpoints <- ZIO.service[ArticlesEndpoints]
          endpoint = articlesEndpoints.update
          authHeader <- newValidAuthorizationHeader()
          response <- basicRequest
            .put(uri"http://test.com/api/articles/test-slug")
            .body(
              ArticleUpdate(
                ArticleUpdateData(
                  slug = Option("test-slug-2"),
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
              ArticleRow(
                slug = "test-slug",
                title = "Test slug",
                description = "description",
                body = "body",
                createdAt = Instant.now,
                updatedAt = Instant.now,
                authorId = 1
              )
            )
            _ <- repo.add(
              ArticleRow(
                slug = "test-slug-2",
                title = "Test slug 2",
                description = "description",
                body = "body",
                createdAt = Instant.now,
                updatedAt = Instant.now,
                authorId = 1
              )
            )
            articlesEndpoints <- ZIO.service[ArticlesEndpoints]
            endpoint = articlesEndpoints.update
            authHeader <- newValidAuthorizationHeader()
            response <- basicRequest
              .put(uri"http://test.com/api/articles/test-slug")
              .body(
                ArticleUpdate(
                  ArticleUpdateData(
                    slug = Option("test-slug-2"),
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
      Configuration.live,
      AuthService.live,
      UsersRepository.live,
      ArticlesRepository.live,
      ArticlesService.live,
      ArticlesEndpoints.live,
      BaseEndpoints.live,
      ProfilesRepository.live,
      ProfilesService.live,
      testDbLayerWithEmptyDb
    )
  )
