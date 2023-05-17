package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.core.api.*
import com.softwaremill.realworld.articles.core.{Article, ArticleAuthor, ArticlesServerEndpoints}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.users.api.UserRegisterData
import com.softwaremill.realworld.users.{Profile, UsersRepository}
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.{asJson, zioJsonBodySerializer}
import sttp.client3.{HttpError, Response, ResponseException, basicRequest}
import sttp.model.Uri
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertTrue, assertZIO}

import scala.collection.immutable.Map

object ArticleEndpointTestSupport:

  def callGetListArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticlesListResponse]] =
    val listArticleEndpoint = ZIO
      .service[ArticlesServerEndpoints]
      .map(_.listArticlesServerEndpoint)

    executeRequest(authorizationHeaderOpt, uri, listArticleEndpoint)

  def callGetFeedArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticlesListResponse]] =
    val feedArticleEndpoint = ZIO
      .service[ArticlesServerEndpoints]
      .map(_.feedArticlesServerEndpoint)

    executeRequest(authorizationHeaderOpt, uri, feedArticleEndpoint)

  private def executeRequest(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri,
      endpoint: ZIO[ArticlesServerEndpoints, Nothing, ZServerEndpoint[Any, Any]]
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticlesListResponse]] =
    endpoint
      .flatMap { endpoint =>
        val requestWithUri = basicRequest
          .get(uri)

        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[ArticlesListResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callGetArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticleResponse]] =
    ZIO
      .service[ArticlesServerEndpoints]
      .map(_.getServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .headers(authorizationHeader)
          .response(asJson[ArticleResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callDeleteArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, Response[Either[String, String]]] =
    ZIO
      .service[ArticlesServerEndpoints]
      .map(_.deleteServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .delete(uri)
          .headers(authorizationHeader)
          .send(backendStub(endpoint))
      }

  def callCreateArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticleResponse]] =
    ZIO
      .service[ArticlesServerEndpoints]
      .map(_.createServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .body(ArticleCreateRequest(createData))
          .headers(authorizationHeader)
          .response(asJson[ArticleResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callUpdateArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdateData
  ): ZIO[ArticlesServerEndpoints, Throwable, Either[ResponseException[String, String], ArticleResponse]] =
    ZIO
      .service[ArticlesServerEndpoints]
      .map(_.updateServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .put(uri)
          .headers(authorizationHeader)
          .body(ArticleUpdateRequest(updateData))
          .response(asJson[ArticleResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfFilterErrorOccur(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetListArticles(authorizationHeaderOpt, uri)
    )(
      isLeft(
        equalTo(
          HttpError(
            """{"errors":{"tag":["Invalid value for: query parameter tag (expected value to match: \\w+, but got: \"invalid-tag\")"]}}""",
            sttp.model.StatusCode.UnprocessableEntity
          )
        )
      )
    )

  def checkIfPaginationErrorOccur(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetListArticles(authorizationHeaderOpt, uri)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"errors\":{\"limit\":[\"Invalid value for: query parameter limit\"]}}",
            sttp.model.StatusCode.UnprocessableEntity
          )
        )
      )
    )

  def checkIfPaginationErrorOccurInFeed(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetFeedArticles(authorizationHeaderOpt, uri)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"errors\":{\"limit\":[\"Invalid value for: query parameter limit\"]}}",
            sttp.model.StatusCode.UnprocessableEntity
          )
        )
      )
    )

  def updateAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateArticle(authorizationHeader, uri, updateData)
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

  def createAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callCreateArticle(authorizationHeader, uri, createData)
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

  def checkIfNonExistentArticleErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(callGetArticle(authorizationHeader, uri))(
      isLeft(
        equalTo(
          HttpError("{\"error\":\"Article with slug unknown-article doesn't exist.\"}", sttp.model.StatusCode(404))
        )
      )
    )

  def checkIfEmptyFieldsErrorOccurInCreate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callCreateArticle(authorizationHeader, uri, createData)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"errors\":{\"body\":[\"Invalid value for: body (" +
              "expected article.title to have length greater than or equal to 1, but got: \\\"\\\", " +
              "expected article.description to have length greater than or equal to 1, but got: \\\"\\\", " +
              "expected article.body to have length greater than or equal to 1, but got: \\\"\\\", " +
              "expected article.tagList to pass validation: each element in the tagList is " +
              "expected to have a length greater than or equal to 1, but got: List())\"]}}",
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def checkIfEmptyFieldsErrorOccurInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateArticle(authorizationHeader, uri, updateData)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"errors\":{\"body\":[\"Invalid value for: body (" +
              "expected article.title to have length greater than or equal to 1, but got: \\\"\\\", " +
              "expected article.description to have length greater than or equal to 1, but got: \\\"\\\", " +
              "expected article.body to have length greater than or equal to 1, but got: \\\"\\\")\"]}}",
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def checkIfArticleListIsEmpty(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetListArticles(authorizationHeaderOpt, uri)
    )(
      isRight(
        equalTo(
          ArticlesListResponse(
            articles = List.empty[Article],
            articlesCount = 0
          )
        )
      )
    )

  def checkPagination(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesListOrError <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesListOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(1)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            exists(
              (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
                && hasField("title", _.title, equalTo("How to train your dragon 2"))
                && hasField("description", _.description, equalTo("So toothless"))
                && hasField("body", _.body, equalTo("Its a dragon"))
                && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
                && hasField("favorited", _.favorited, isFalse)
                && hasField("favoritesCount", _.favoritesCount, equalTo(1))
                && hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                    "following",
                    _.following,
                    isFalse
                  )
                )
            )
          )
      )
    }

  def checkFeedPagination(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesFeedOrError <- callGetFeedArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesFeedOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(1)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            exists(
              (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
                && hasField("title", _.title, equalTo("How to train your dragon 2"))
                && hasField("description", _.description, equalTo("So toothless"))
                && hasField("body", _.body, equalTo("Its a dragon"))
                && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
                && hasField("favorited", _.favorited, isTrue)
                && hasField("favoritesCount", _.favoritesCount, equalTo(1))
                && hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                    "following",
                    _.following,
                    isTrue
                  )
                )
            )
          )
      )
    }

  def checkFilters(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesListOrError <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesListOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(1)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            exists(
              (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
                && hasField("title", _.title, equalTo("How to train your dragon 2"))
                && hasField("description", _.description, equalTo("So toothless"))
                && hasField("body", _.body, equalTo("Its a dragon"))
                && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
                && hasField("favorited", _.favorited, isFalse)
                && hasField("favoritesCount", _.favoritesCount, equalTo(1))
                && hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                    "following",
                    _.following,
                    isFalse
                  )
                )
            )
          )
      )
    }

  def listAvailableArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesListOrError <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesListOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(3)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            exists(
              (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
                && hasField("title", _.title, equalTo("How to train your dragon"))
                && hasField("description", _.description, equalTo("Ever wonder how?"))
                && hasField("body", _.body, equalTo("It takes a Jacobian"))
                && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
                && hasField("favorited", _.favorited, if (authorizationHeaderOpt.isDefined) isTrue else isFalse)
                && hasField("favoritesCount", _.favoritesCount, equalTo(2))
                && hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                    "following",
                    _.following,
                    isFalse
                  )
                )
            ) &&
              exists(
                (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
                  && hasField("title", _.title, equalTo("How to train your dragon 2"))
                  && hasField("description", _.description, equalTo("So toothless"))
                  && hasField("body", _.body, equalTo("Its a dragon"))
                  && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
                  && hasField("favorited", _.favorited, isFalse)
                  && hasField("favoritesCount", _.favoritesCount, equalTo(1))
                  && hasField(
                    "author",
                    _.author,
                    (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                      "following",
                      _.following,
                      isFalse
                    )
                  )
              ) &&
              exists(
                (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
                  && hasField("title", _.title, equalTo("How to train your dragon 3"))
                  && hasField("description", _.description, equalTo("The tagless one"))
                  && hasField("body", _.body, equalTo("Its not a dragon"))
                  && hasField("tagList", _.tagList, equalTo(List()))
                  && hasField("favorited", _.favorited, isFalse)
                  && hasField("favoritesCount", _.favoritesCount, equalTo(0))
                  && hasField(
                    "author",
                    _.author,
                    (hasField("username", _.username, equalTo("john")): Assertion[ArticleAuthor]) && hasField(
                      "following",
                      _.following,
                      if (authorizationHeaderOpt.isDefined) isTrue else isFalse
                    )
                  )
              )
          )
      )
    }

  def listFeedAvailableArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesFeedOrError <- callGetFeedArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesFeedOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(3)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            exists(
              (hasField("slug", _.slug, equalTo("how-to-train-your-dragon")): Assertion[Article])
                && hasField("title", _.title, equalTo("How to train your dragon"))
                && hasField("description", _.description, equalTo("Ever wonder how?"))
                && hasField("body", _.body, equalTo("It takes a Jacobian"))
                && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
                && hasField("favorited", _.favorited, isTrue)
                && hasField("favoritesCount", _.favoritesCount, equalTo(2))
                && hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                    "following",
                    _.following,
                    isTrue
                  )
                )
            ) &&
              exists(
                (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
                  && hasField("title", _.title, equalTo("How to train your dragon 2"))
                  && hasField("description", _.description, equalTo("So toothless"))
                  && hasField("body", _.body, equalTo("Its a dragon"))
                  && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
                  && hasField("favorited", _.favorited, isTrue)
                  && hasField("favoritesCount", _.favoritesCount, equalTo(1))
                  && hasField(
                    "author",
                    _.author,
                    (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                      "following",
                      _.following,
                      isTrue
                    )
                  )
              ) &&
              exists(
                (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-5")): Assertion[Article])
                  && hasField("title", _.title, equalTo("How to train your dragon 5"))
                  && hasField("description", _.description, equalTo("The tagfull one"))
                  && hasField("body", _.body, equalTo("Its a blue dragon"))
                  && hasField("tagList", _.tagList, equalTo(List()))
                  && hasField("favorited", _.favorited, isFalse)
                  && hasField("favoritesCount", _.favoritesCount, equalTo(0))
                  && hasField(
                    "author",
                    _.author,
                    (hasField("username", _.username, equalTo("bill")): Assertion[ArticleAuthor]) && hasField(
                      "following",
                      _.following,
                      isTrue
                    )
                  )
              )
          )
      )
    }

  def checkArticlesListAfterDeletion(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    for {
      articlesListOrError <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield zio.test.assert(articlesListOrError.toOption) {
      isSome(
        (hasField("articlesCount", _.articlesCount, equalTo(1)): Assertion[ArticlesListResponse]) &&
          hasField(
            "articles",
            _.articles,
            !exists(hasField("slug", _.slug, equalTo("how-to-train-your-dragon-3")): Assertion[Article])
          )
      )
    }

  def getAndCheckExistingArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetArticle(authorizationHeader, uri)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username, equalTo("john")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
      )
    )

  def createAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callCreateArticle(authorizationHeader, uri, createData)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[Article])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
      )
    )

  def updateAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdateData
  ): ZIO[ArticlesServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateArticle(authorizationHeader, uri, updateData)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("updated-slug")): Assertion[Article])
            && hasField("title", _.title, equalTo("Updated slug"))
            && hasField("description", _.description, equalTo("updated description"))
            && hasField("body", _.body, equalTo("updated body"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField(
              "author",
              _.author,
              (hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
      )
    )
