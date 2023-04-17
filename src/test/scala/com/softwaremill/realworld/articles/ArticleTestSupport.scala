package com.softwaremill.realworld.articles

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.{asJson, zioJsonBodySerializer}
import sttp.client3.{HttpError, Response, ResponseException, basicRequest}
import sttp.model.Uri
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO
import zio.test.Assertion.{equalTo, isLeft, isRight}
import zio.test.{TestResult, assertTrue, assertZIO}

import java.time.Instant
import scala.collection.immutable.Map

object ArticleTestSupport {

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

  def checkPagination(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callGetListArticles(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.contains(
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
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.contains(
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
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 1 &&
      articlesList.articles.contains(
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
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 3 &&
      articlesList.articles.contains(
        ArticleData(
          "how-to-train-your-dragon",
          "How to train your dragon",
          "Ever wonder how?",
          "It takes a Jacobian",
          List("dragons", "training"),
          Instant.ofEpochMilli(1455765776637L),
          Instant.ofEpochMilli(1455767315824L),
          false,
          2,
          ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
        )
      ) &&
      articlesList.articles.contains(
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
      ) &&
      articlesList.articles.contains(
        ArticleData(
          "how-to-train-your-dragon-3",
          "How to train your dragon 3",
          "The tagless one",
          "Its not a dragon",
          List(),
          Instant.ofEpochMilli(1455765776637L),
          Instant.ofEpochMilli(1455767315824L),
          false,
          0,
          ArticleAuthor(
            "john",
            Some("I no longer work at statefarm"),
            Some("https://i.stack.imgur.com/xHWG8.jpg"),
            following = false
          )
        )
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
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 3 &&
      articlesList.articles.contains(
        ArticleData(
          "how-to-train-your-dragon",
          "How to train your dragon",
          "Ever wonder how?",
          "It takes a Jacobian",
          List("dragons", "training"),
          Instant.ofEpochMilli(1455765776637L),
          Instant.ofEpochMilli(1455767315824L),
          false,
          2,
          ArticleAuthor("jake", Some("I work at statefarm"), Some("https://i.stack.imgur.com/xHWG8.jpg"), following = false)
        )
      ) &&
      articlesList.articles.contains(
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
      ) &&
      articlesList.articles.contains(
        ArticleData(
          "how-to-train-your-dragon-5",
          "How to train your dragon 5",
          "The tagfull one",
          "Its a blue dragon",
          List(),
          Instant.ofEpochMilli(1455765776637L),
          Instant.ofEpochMilli(1455767315824L),
          false,
          0,
          ArticleAuthor(
            "bill",
            Some("I work in the bank"),
            Some("https://i.stack.imgur.com/xHWG8.jpg"),
            following = false
          )
        )
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
      import com.softwaremill.realworld.common.model.UserDiff.{*, given}

      val articlesList = result.toOption.get

      articlesList.articlesCount == 2 && !articlesList.articles.contains(
        ArticleData(
          "how-to-train-your-dragon-3",
          "How to train your dragon 3",
          "The tagless one",
          "Its not a dragon",
          List(),
          Instant.ofEpochMilli(1455765776637L),
          Instant.ofEpochMilli(1455767315824L),
          false,
          0,
          ArticleAuthor(
            "john",
            Some("I no longer work at statefarm"),
            Some("https://i.stack.imgur.com/xHWG8.jpg"),
            following = false
          )
        )
      )
    }
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

  def getAndCheckExistingArticle(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callGetArticle(authorizationHeader, uri)
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

  def createAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callCreateArticle(authorizationHeader, uri, createData)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}

      compare(
        result.toOption.get,
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
  }

  def createAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      createData: ArticleCreateData
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callCreateArticle(authorizationHeader, uri, createData)
    )(
      // TODO there must be better way to implement this...
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

  def updateAndCheckArticle(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdate
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- callUpdateArticle(authorizationHeader, uri, updateData)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      import com.softwaremill.realworld.common.model.ArticleDiff.{*, given}

      compare(
        result.toOption.get,
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
  }

  def updateAndCheckIfInvalidNameErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateData: ArticleUpdate
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      callUpdateArticle(authorizationHeader, uri, updateData)
    )(
      // TODO there must be better way to implement this...
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
}
