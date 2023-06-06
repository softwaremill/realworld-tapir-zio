package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.CommentsRepository
import com.softwaremill.realworld.articles.core.ArticleDbTestSupport.*
import com.softwaremill.realworld.articles.core.ArticleEndpointTestSupport.*
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData, ArticlesEndpoints}
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.DbData.{exampleArticle2, exampleUser2}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.ZLayer
import zio.config.ReadError
import zio.test.ZIOSpecDefault

object ArticlesEndpointsSpec extends ZIOSpecDefault:

  override def spec = suite("article endpoints tests")(
    suite("check articles list")(
      suite("with token auth header")(
        test("validation failed on filter") {
          for {
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfFilterErrorOccur(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?tag=invalid-tag"
            )
          } yield result
        },
        test("validation failed on pagination") {
          for {
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfPaginationErrorOccur(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
            )
          } yield result
        },
        test("check pagination") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkPagination(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?limit=1&offset=1"
            )
          } yield result
        },
        test("check filters") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkFilters(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
            )
          } yield result
        },
        test("return empty list") {
          for {
            _ <- prepareDataForListingEmptyList
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfArticleListIsEmpty(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/articles")
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidTokenAuthorizationHeader()
            result <- listAvailableArticles(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles"
            )
          } yield result
        }
      ),
      suite("with bearer auth header")(
        test("validation failed on filter") {
          for {
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfFilterErrorOccur(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?tag=invalid-tag"
            )
          } yield result
        },
        test("validation failed on pagination") {
          for {
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfPaginationErrorOccur(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?limit=invalid-limit&offset=invalid-offset"
            )
          } yield result
        },
        test("check pagination") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkPagination(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?limit=1&offset=1"
            )
          } yield result
        },
        test("check filters") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkFilters(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles?author=jake&favorited=john&tag=goats"
            )
          } yield result
        },
        test("return empty list") {
          for {
            _ <- prepareDataForListingEmptyList
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfArticleListIsEmpty(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/articles")
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForListingArticles
            authHeader <- getValidBearerAuthorizationHeader()
            result <- listAvailableArticles(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles"
            )
          } yield result
        }
      ),
      suite("with no header")(
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
        test("return empty list") {
          checkIfArticleListIsEmpty(authorizationHeaderOpt = None, uri = uri"http://test.com/api/articles")
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
      suite("with token auth header")(
        test("validation failed on pagination") {
          for {
            authHeader <- getValidTokenAuthorizationHeader(email = "john@example.com")
            result <- checkIfPaginationErrorOccurInFeed(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed?limit=invalid-limit&offset=invalid-offset"
            )
          } yield result
        },
        test("check pagination") {
          for {
            _ <- prepareDataForFeedingArticles
            authHeader <- getValidTokenAuthorizationHeader(email = "john@example.com")
            result <- checkFeedPagination(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed?limit=1&offset=1"
            )
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForFeedingArticles
            authHeader <- getValidTokenAuthorizationHeader(email = "john@example.com")
            result <- listFeedAvailableArticles(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed"
            )
          } yield result
        }
      ),
      suite("with bearer auth header")(
        test("validation failed on pagination") {
          for {
            authHeader <- getValidBearerAuthorizationHeader(email = "john@example.com")
            result <- checkIfPaginationErrorOccurInFeed(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed?limit=invalid-limit&offset=invalid-offset"
            )
          } yield result
        },
        test("check pagination") {
          for {
            _ <- prepareDataForFeedingArticles
            authHeader <- getValidBearerAuthorizationHeader(email = "john@example.com")
            result <- checkFeedPagination(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed?limit=1&offset=1"
            )
          } yield result
        },
        test("list available articles") {
          for {
            _ <- prepareDataForFeedingArticles
            authHeader <- getValidBearerAuthorizationHeader(email = "john@example.com")
            result <- listFeedAvailableArticles(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/feed"
            )
          } yield result
        }
      )
    ),
    suite("check articles get")(
      suite("with token auth header")(
        test("article not exists") {
          for {
            _ <- prepareDataForGettingArticle
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfNonExistentArticleErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/unknown-article"
            )
          } yield result
        },
        test("get existing article") {
          for {
            _ <- prepareDataForGettingArticle
            authHeader <- getValidTokenAuthorizationHeader()
            result <- getAndCheckExistingArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2"
            )
          } yield result
        }
      ),
      suite("with bearer auth header")(
        test("article not exists") {
          for {
            _ <- prepareDataForGettingArticle
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfNonExistentArticleErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/unknown-article"
            )
          } yield result
        },
        test("get existing article") {
          for {
            _ <- prepareDataForGettingArticle
            authHeader <- getValidBearerAuthorizationHeader()
            result <- getAndCheckExistingArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2"
            )
          } yield result
        }
      )
    ),
    suite("check article creation")(
      suite("with token auth header")(
        test("return empty string fields error") {
          for {
            _ <- prepareDataForArticleCreation
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInCreate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = ArticleCreateData(
                title = "",
                description = "",
                body = "",
                tagList = Some(List(""))
              )
            )
          } yield result
        },
        test("article creation - check conflict") {
          for {
            _ <- prepareDataForCreatingNameConflict
            authHeader <- getValidTokenAuthorizationHeader()
            result <- createAndCheckIfInvalidNameErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = exampleArticle2
            )
          } yield result
        },
        test("positive article creation") {
          for {
            _ <- prepareDataForArticleCreation
            authHeader <- getValidTokenAuthorizationHeader()
            result <- createAndCheckArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = exampleArticle2
            )
          } yield result
        }
      ),
      suite("with bearer auth header")(
        test("return empty string fields error") {
          for {
            _ <- prepareDataForArticleCreation
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInCreate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = ArticleCreateData(
                title = "",
                description = "",
                body = "",
                tagList = Some(List(""))
              )
            )
          } yield result
        },
        test("article creation - check conflict") {
          for {
            _ <- prepareDataForCreatingNameConflict
            authHeader <- getValidBearerAuthorizationHeader()
            result <- createAndCheckIfInvalidNameErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = exampleArticle2
            )
          } yield result
        },
        test("positive article creation") {
          for {
            _ <- prepareDataForArticleCreation
            authHeader <- getValidBearerAuthorizationHeader()
            result <- createAndCheckArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles",
              createData = exampleArticle2
            )
          } yield result
        }
      )
    ),
    suite("check article deletion")(
      suite("with token auth header")(
        test("positive article deletion")(
          for {
            _ <- prepareDataForArticleDeletion
            authHeader <- getValidTokenAuthorizationHeader(exampleUser2.email)
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
      suite("with bearer auth header")(
        test("positive article deletion")(
          for {
            _ <- prepareDataForArticleDeletion
            authHeader <- getValidBearerAuthorizationHeader(exampleUser2.email)
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
      )
    ),
    suite("update article")(
      suite("with token auth header")(
        test("return empty string fields error") {
          for {
            _ <- prepareDataForArticleUpdating
            authHeader <- getValidTokenAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some(""),
                description = Some(""),
                body = Some("")
              )
            )
          } yield result
        },
        test("article update - check conflict") {
          for {
            _ <- prepareDataForUpdatingNameConflict
            authHeader <- getValidTokenAuthorizationHeader()
            result <- updateAndCheckIfInvalidNameErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some("How to train your dragon 2"),
                description = Some("updated description"),
                body = Some("updated body")
              )
            )
          } yield result
        },
        test("positive article update") {
          for {
            _ <- prepareDataForArticleUpdating
            authHeader <- getValidTokenAuthorizationHeader()
            result <- updateAndCheckArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some("Updated slug"),
                description = Some("updated description"),
                body = Some("updated body")
              )
            )
          } yield result
        }
      ),
      suite("with bearer auth header")(
        test("return empty string fields error") {
          for {
            _ <- prepareDataForArticleUpdating
            authHeader <- getValidBearerAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some(""),
                description = Some(""),
                body = Some("")
              )
            )
          } yield result
        },
        test("article update - check conflict") {
          for {
            _ <- prepareDataForUpdatingNameConflict
            authHeader <- getValidBearerAuthorizationHeader()
            result <- updateAndCheckIfInvalidNameErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some("How to train your dragon 2"),
                description = Some("updated description"),
                body = Some("updated body")
              )
            )
          } yield result
        },
        test("positive article update") {
          for {
            _ <- prepareDataForArticleUpdating
            authHeader <- getValidBearerAuthorizationHeader()
            result <- updateAndCheckArticle(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon",
              updateData = ArticleUpdateData(
                title = Some("Updated slug"),
                description = Some("updated description"),
                body = Some("updated body")
              )
            )
          } yield result
        }
      )
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    BaseEndpoints.live,
    UsersRepository.live,
    ArticlesRepository.live,
    ArticlesService.live,
    ArticlesEndpoints.live,
    ArticlesServerEndpoints.live,
    CommentsRepository.live,
    testDbLayerWithEmptyDb
  )
