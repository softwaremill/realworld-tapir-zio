package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.ArticleEndpointTestSupport.*
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.api.UserRegisterData
import com.softwaremill.realworld.users.{UsersRepository, UsersService}
import com.softwaremill.realworld.utils.DbData.exampleArticle2
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import sttp.model.Uri
import zio.ZLayer
import zio.config.ReadError
import zio.test.ZIOSpecDefault

object ArticlesEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("article endpoints tests")(
    suite("check articles list")(
      suite("with auth header")(
        test("return empty list") {
          for {
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfArticleListIsEmpty(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/articles")
          } yield result
        },
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
            _ <- prepareDataForListingArticles
            authHeader <- getValidAuthorizationHeader()
            result <- checkPagination(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?limit=1&offset=1"
            )
          } yield result
        },
        test("check filters") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidAuthorizationHeader()
            result <- checkFilters(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
            )
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidAuthorizationHeader()
            result <- listAvailableArticles(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles"
            )
          } yield result
        }
      ),
      suite("with no header")(
        test("return empty list") {
          checkIfArticleListIsEmpty(authorizationHeaderOpt = None, uri = uri"http://test.com/api/articles")
        },
        test("validation failed on filter") {
          for {
            result <- checkIfFilterErrorOccur(
              authorizationHeaderOpt = None,
              uri = uri"http://test.com/api/articles?tag=invalid-tag"
            )
          } yield result
        },
        test("validation failed on pagination") {
          for {
            result <- checkIfPaginationErrorOccur(
              authorizationHeaderOpt = None,
              uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
            )
          } yield result
        },
        test("check pagination") {
          for {
            _ <- prepareDataForListingArticles
            result <- checkPagination(
              authorizationHeaderOpt = None,
              uri = uri"http://test.com/api/articles?limit=1&offset=1"
            )
          } yield result
        },
        test("check filters") {
          for {
            _ <- prepareDataForListingArticles
            result <- checkFilters(
              authorizationHeaderOpt = None,
              uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
            )
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForListingArticles
            result <- listAvailableArticles(
              authorizationHeaderOpt = None,
              uri = uri"http://test.com/api/articles"
            )
          } yield result
        }
      )
    ),
    suite("check articles feed")(
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
          _ <- prepareDataForFeedingArticles
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- checkFeedPagination(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/feed?limit=1&offset=1"
          )
        } yield result
      },
      test("list available articles") {
        for {
          _ <- prepareDataForFeedingArticles
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- listFeedAvailableArticles(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/feed"
          )
        } yield result
      }
    ),
    suite("check articles get")(
      test("article not exists") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkIfNonExistentArticleErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/unknown-article"
          )
        } yield result
      },
      test("get existing article") {
        for {
          _ <- prepareDataForGettingArticle
          authHeader <- getValidAuthorizationHeader()
          result <- getAndCheckExistingArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2"
          )
        } yield result
      }
    ),
    suite("check article creation")(
      test("positive article creation") {
        for {
          _ <- prepareDataForArticleCreation
          authHeader <- getValidAuthorizationHeader()
          result <- createAndCheckArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles",
            createData = exampleArticle2
          )
        } yield result
      },
      test("article creation - check conflict") {
        for {
          _ <- prepareDataForCreatingNameConflict
          authHeader <- getValidAuthorizationHeader()
          result <- createAndCheckIfInvalidNameErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles",
            createData = exampleArticle2
          )
        } yield result
      }
    ),
    suite("check article deletion")(
      test("positive article deletion")(
        for {
          _ <- prepareDataForArticleDeletion
          authHeader <- getValidAuthorizationHeader()
          _ <- callDeleteArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-3"
          )
          result <- checkArticlesListAfterDeletion(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles"
          )
        } yield result
      )
    ),
    suite("update article")(
      test("positive article update") {
        for {
          _ <- prepareDataForArticleUpdating
          authHeader <- getValidAuthorizationHeader()
          result <- updateAndCheckArticle(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
            updateData = ArticleUpdate(
              ArticleUpdateData(
                title = Option("Updated slug"),
                description = Option("updated description"),
                body = Option("updated body")
              )
            )
          )
        } yield result
      },
      test("article update - check conflict") {
        for {
          _ <- prepareDataForUpdatingNameConflict
          authHeader <- getValidAuthorizationHeader()
          result <- updateAndCheckIfInvalidNameErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
            updateData = ArticleUpdate(
              ArticleUpdateData(
                title = Option("How to train your dragon 2"),
                description = Option("updated description"),
                body = Option("updated body")
              )
            )
          )
        } yield result
      }
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    BaseEndpoints.live,
    UsersRepository.live,
    UsersService.live,
    ArticlesRepository.live,
    TagsRepository.live,
    ArticlesService.live,
    ArticlesEndpoints.live,
    testDbLayerWithEmptyDb
  )
