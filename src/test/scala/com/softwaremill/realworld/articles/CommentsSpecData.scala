package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.comments.{Comment, CommentCreate, CommentCreateData, CommentData, CommentsList}
import com.softwaremill.realworld.profiles.ProfileData
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.{Response, ResponseException, basicRequest}
import sttp.client3.ziojson.asJson
import sttp.model.Uri
import sttp.tapir.json.zio.jsonBody
import zio.ZIO
import zio.test.Assertion.{equalTo, isRight}
import zio.test.{TestResult, assertTrue, assertZIO}
import sttp.client3.ziojson.zioJsonBodySerializer

import java.time.Instant

object CommentsSpecData {

  def getCommentsFromArticleRequest(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, Either[ResponseException[String, String], CommentsList]] = {

    ZIO
      .service[ArticlesEndpoints]
      .map(_.getCommentsFromArticle)
      .flatMap { endpoint =>

        val requestWithUri = basicRequest
          .get(uri)

        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[CommentsList])
          .send(backendStub(endpoint))
          .map(_.body)
      }
  }

  def addCommentRequest(
      authorizationHeader: Map[String, String],
      uri: Uri,
      bodyComment: String
  ): ZIO[ArticlesEndpoints, Throwable, Either[ResponseException[String, String], Comment]] = {

    ZIO
      .service[ArticlesEndpoints]
      .map(_.addComment)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .body(CommentCreate(comment = CommentCreateData(body = bodyComment)))
          .headers(authorizationHeader)
          .response(asJson[Comment])
          .send(backendStub(endpoint))
          .map(_.body)
      }
  }

  def deleteCommentRequest(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, Response[Either[String, String]]] = {

    ZIO
      .service[ArticlesEndpoints]
      .map(_.deleteComment)
      .flatMap { endpoint =>
        basicRequest
          .delete(uri)
          .headers(authorizationHeader)
          .send(backendStub(endpoint))
      }
  }

  def checkIfCommentsListIsEmpty(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    assertZIO(
      getCommentsFromArticleRequest(authorizationHeaderOpt, uri)
    )(
      isRight(
        equalTo(
          CommentsList(
            comments = List.empty[CommentData]
          )
        )
      )
    )
  }

  def createAndCheckComment(
      authorizationHeader: Map[String, String],
      uri: Uri,
      body: String
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- addCommentRequest(authorizationHeader, uri, body)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val comment = result.toOption.get.comment

      comment.body == "Amazing article!" && comment.author.equals(
        ProfileData(
          username = "john",
          bio = Some("I no longer work at statefarm"),
          image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
          following = false
        )
      )
    }
  }

  def checkCommentsList(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- getCommentsFromArticleRequest(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val listComments = result.toOption.get.comments

      val firstComment = listComments.head
      val secondComment = listComments.last

      listComments.size == 2 &&
      firstComment.body == "Thank you so much!" && firstComment.author.equals(
        ProfileData(
          username = "jake",
          bio = Some("I work at statefarm"),
          image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
          following = authorizationHeaderOpt.isDefined
        )
      ) && secondComment.body == "Great article!" && secondComment.author.equals(
        ProfileData(
          username = "michael",
          bio = Some("I no longer work in the bank"),
          image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
          following = false
        )
      )
    }
  }

  def checkCommentsListAfterDelete(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      result <- getCommentsFromArticleRequest(authorizationHeaderOpt, uri)
    } yield assertTrue {
      // TODO there must be better way to implement this...
      val listComments = result.toOption.get.comments
      val firstComment = listComments.head

      listComments.size == 1 &&
      firstComment.body == "Not bad." && firstComment.author.equals(
        ProfileData(
          username = "michael",
          bio = Some("I no longer work in the bank"),
          image = Some("https://i.stack.imgur.com/xHWG8.jpg"),
          following = false
        )
      )
    }
  }
}
