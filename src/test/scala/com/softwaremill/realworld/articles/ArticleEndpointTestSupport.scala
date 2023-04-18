package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.profiles.ProfilesRepository
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.{UserRegisterData, UsersRepository}
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.{asJson, zioJsonBodySerializer}
import sttp.client3.{HttpError, Response, ResponseException, basicRequest}
import sttp.model.Uri
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO
import zio.test.Assertion.{equalTo, hasField, isFalse, isLeft, isRight}
import zio.test.{Assertion, TestResult, assertTrue, assertZIO}

import java.time.Instant
import scala.collection.immutable.Map

object ArticleEndpointTestSupport {

  def callGetListArticles(authorizationHeaderOpt: Option[Map[String, String]], uri: Uri) =
    val listArticleEndpoint = ZIO
      .service[ArticlesEndpoints]
      .map(_.listArticles)

    executeRequest(authorizationHeaderOpt, uri, listArticleEndpoint)

  def callGetFeedArticles(authorizationHeaderOpt: Option[Map[String, String]], uri: Uri) =
    val feedArticleEndpoint = ZIO
      .service[ArticlesEndpoints]
      .map(_.feedArticles)

    executeRequest(authorizationHeaderOpt, uri, feedArticleEndpoint)

  private def executeRequest(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri,
      endpoint: ZIO[ArticlesEndpoints, Nothing, ZServerEndpoint[Any, Any]]
  ): ZIO[ArticlesEndpoints, Throwable, Either[ResponseException[String, String], ArticlesList]] = {

    endpoint
      .flatMap { endpoint =>
        val requestWithUri = basicRequest
          .get(uri)

        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[ArticlesList])
          .send(backendStub(endpoint))
          .map(_.body)
      }
  }

  def callGetArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ) =
    ZIO
      .service[ArticlesEndpoints]
      .map(_.get)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .headers(authorizationHeader)
          .response(asJson[Article])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callDeleteArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, Response[Either[String, String]]] = {

    ZIO
      .service[ArticlesEndpoints]
      .map(_.delete)
      .flatMap { endpoint =>
        basicRequest
          .delete(uri)
          .headers(authorizationHeader)
          .send(backendStub(endpoint))
      }
  }

  def callCreateArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesEndpoints, Throwable, Either[ResponseException[String, String], Article]] = {
    ZIO
      .service[ArticlesEndpoints]
      .map(_.create)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .body(ArticleCreate(createData))
          .headers(authorizationHeader)
          .response(asJson[Article])
          .send(backendStub(endpoint))
          .map(_.body)
      }
  }

  def callUpdateArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdate
  ): ZIO[ArticlesEndpoints, Throwable, Either[ResponseException[String, String], Article]] = {
    ZIO
      .service[ArticlesEndpoints]
      .map(_.update)
      .flatMap { endpoint =>
        basicRequest
          .put(uri)
          .body(updateData)
          .headers(authorizationHeader)
          .response(asJson[Article])
          .send(backendStub(endpoint))
          .map(_.body)
      }
  }

  def checkIfFilterErrorOccur(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

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
  }

  def checkIfPaginationErrorOccur(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

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
  }

  def checkIfPaginationErrorOccurInFeed(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

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
  }

  def updateAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdate
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

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
  }

  def checkIfArticleListIsEmpty(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callGetListArticles(authorizationHeaderOpt, uri)
    )(
      isRight(
        equalTo(
          ArticlesList(
            articles = List.empty[ArticleData],
            articlesCount = 0
          )
        )
      )
    )
  }

  def createAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

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
  }

  def checkIfNonExistentArticleErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(callGetArticle(authorizationHeader, uri))(
      isLeft(
        equalTo(
          HttpError("{\"error\":\"Article with slug unknown-article doesn't exist.\"}", sttp.model.StatusCode(404))
        )
      )
    )
  }

  def checkPagination(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def checkFeedPagination(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetFeedArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def checkFilters(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      )
    }
  }

  def listAvailableArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 3 &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon" &&
          article.title == "How to train your dragon" &&
          article.description == "Ever wonder how?" &&
          article.body == "It takes a Jacobian" &&
          article.tagList == List("dragons", "training") &&
          !article.favorited &&
          article.favoritesCount == 2 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-3" &&
          article.title == "How to train your dragon 3" &&
          article.description == "The tagless one" &&
          article.body == "Its not a dragon" &&
          article.tagList == List() &&
          !article.favorited &&
          article.favoritesCount == 0 &&
          article.author.username == "john" &&
          !article.author.following
      )
    }
  }

  def listFeedAvailableArticles(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetFeedArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 3 &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon" &&
          article.title == "How to train your dragon" &&
          article.description == "Ever wonder how?" &&
          article.body == "It takes a Jacobian" &&
          article.tagList == List("dragons", "training") &&
          !article.favorited &&
          article.favoritesCount == 2 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-2" &&
          article.title == "How to train your dragon 2" &&
          article.description == "So toothless" &&
          article.body == "Its a dragon" &&
          article.tagList == List("dragons", "goats", "training") &&
          !article.favorited &&
          article.favoritesCount == 1 &&
          article.author.username == "jake" &&
          !article.author.following
      ) &&
      articlesList.articles.exists(article =>
        article.slug == "how-to-train-your-dragon-5" &&
          article.title == "How to train your dragon 5" &&
          article.description == "The tagfull one" &&
          article.body == "Its a blue dragon" &&
          article.tagList == Nil &&
          !article.favorited &&
          article.favoritesCount == 0 &&
          article.author.username == "bill" &&
          !article.author.following
      )
    }
  }

  def checkArticlesListAfterDeletion(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val articlesList = result.toOption.get

      articlesList.articlesCount == 2 && !articlesList.articles.exists(article => article.slug == "how-to-train-your-dragon-3")
    }
  }

  def getAndCheckExistingArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callGetArticle(authorizationHeader, uri)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[ArticleData])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(List("dragons", "goats", "training")))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(1))
            && hasField("author", _.author, hasField("username", _.username, equalTo("john")): Assertion[ArticleAuthor])
        )
      )
    )
  }

  def createAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callCreateArticle(authorizationHeader, uri, createData)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("how-to-train-your-dragon-2")): Assertion[ArticleData])
            && hasField("title", _.title, equalTo("How to train your dragon 2"))
            && hasField("description", _.description, equalTo("So toothless"))
            && hasField("body", _.body, equalTo("Its a dragon"))
            && hasField("tagList", _.tagList, equalTo(Nil))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        )
      )
    )
  }

  def updateAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdate
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callUpdateArticle(authorizationHeader, uri, updateData)
    )(
      isRight(
        hasField(
          "article",
          _.article,
          (hasField("slug", _.slug, equalTo("updated-slug")): Assertion[ArticleData])
            && hasField("title", _.title, equalTo("Updated slug"))
            && hasField("description", _.description, equalTo("updated description"))
            && hasField("body", _.body, equalTo("updated body"))
            && hasField("tagList", _.tagList, equalTo(Nil))
            && hasField("favorited", _.favorited, isFalse)
            && hasField("favoritesCount", _.favoritesCount, equalTo(0))
            && hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ArticleAuthor])
        )
      )
    )
  }
}
