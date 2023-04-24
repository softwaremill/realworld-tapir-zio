package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.ArticlesEndpoints
import com.softwaremill.realworld.articles.comments.{Comment, CommentCreate, CommentCreateData, CommentData, CommentsList}
import com.softwaremill.realworld.profiles.ProfileData
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.{asJson, zioJsonBodySerializer}
import sttp.client3.{Response, ResponseException, basicRequest}
import sttp.model.Uri
import sttp.tapir.json.zio.jsonBody
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertTrue, assertZIO}

import java.time.Instant

object CommentTestSupport:

  def callGetCommentsFromArticle(
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

  def callAddComment(
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
      callGetCommentsFromArticle(authorizationHeaderOpt, uri)
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
      comment <- callAddComment(authorizationHeader, uri, body)
    } yield zio.test.assert(comment.toOption) {
      isSome(
        hasField(
          "comment",
          _.comment,
          (hasField("body", _.body, equalTo("Amazing article!")): Assertion[CommentData]) &&
            hasField("author", _.author, hasField("username", _.username, equalTo("john")): Assertion[ProfileData])
        )
      )
    }
  }

  def checkCommentsList(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      commentsList <- callGetCommentsFromArticle(authorizationHeaderOpt, uri)
    } yield zio.test.assert(commentsList.toOption) {
      isSome(
        hasField(
          "comments",
          _.comments,
          hasSize(equalTo(2)) &&
            exists(
              (hasField("body", _.body, equalTo("Thank you so much!")): Assertion[CommentData]) &&
                hasField("author", _.author, hasField("username", _.username, equalTo("jake")): Assertion[ProfileData])
            ) &&
            exists(
              (hasField("body", _.body, equalTo("Great article!")): Assertion[CommentData]) &&
                hasField("author", _.author, hasField("username", _.username, equalTo("michael")): Assertion[ProfileData])
            )
        )
      )
    }
  }

  def checkCommentsListAfterDelete(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ArticlesEndpoints, Throwable, TestResult] = {

    for {
      commentsList <- callGetCommentsFromArticle(authorizationHeaderOpt, uri)
    } yield zio.test.assert(commentsList.toOption) {

      isSome(
        hasField(
          "comments",
          _.comments,
          hasSize(equalTo(1)) &&
            exists(
              (hasField("body", _.body, equalTo("Not bad.")): Assertion[CommentData]) &&
                hasField("author", _.author, hasField("username", _.username, equalTo("michael")): Assertion[ProfileData])
            )
        )
      )
    }
  }
