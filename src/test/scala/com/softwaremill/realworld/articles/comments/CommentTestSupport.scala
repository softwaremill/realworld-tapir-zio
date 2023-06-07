package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.comments.api.*
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.{asJson, zioJsonBodySerializer}
import sttp.client3.{HttpError, ResponseException, basicRequest}
import sttp.model.Uri
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertZIO}

object CommentTestSupport:

  def callGetCommentsFromArticle(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, Either[ResponseException[String, String], CommentsListResponse]] =
    ZIO
      .service[CommentsServerEndpoints]
      .map(_.getCommentsFromArticleServerEndpoint)
      .flatMap { endpoint =>

        val requestWithUri = basicRequest
          .get(uri)

        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[CommentsListResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callAddComment(
      authorizationHeader: Map[String, String],
      uri: Uri,
      bodyComment: String
  ): ZIO[CommentsServerEndpoints, Throwable, Either[ResponseException[String, String], CommentResponse]] =
    ZIO
      .service[CommentsServerEndpoints]
      .map(_.addCommentServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .body(CommentCreateRequest(comment = CommentCreateData(body = bodyComment)))
          .headers(authorizationHeader)
          .response(asJson[CommentResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def deleteCommentRequest(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, Either[String, String]] =
    ZIO
      .service[CommentsServerEndpoints]
      .map(_.deleteCommentServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .delete(uri)
          .headers(authorizationHeader)
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfCommentsListIsEmpty(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetCommentsFromArticle(authorizationHeaderOpt, uri)
    )(
      isRight(
        equalTo(
          CommentsListResponse(
            comments = List.empty[Comment]
          )
        )
      )
    )

  def checkIfNotAuthorOfCommentErrorOccurInDelete(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    assertZIO(
      deleteCommentRequest(authorizationHeader, uri)
    )(
      isLeft(
        equalTo("{\"error\":\"Can't remove the comment you're not an author of\"}")
      )
    )

  def checkIfCommentNotLinkedToSlugErrorOccurInDelete(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    assertZIO(
      deleteCommentRequest(authorizationHeader, uri)
    )(
      isLeft(
        equalTo("{\"error\":\"Comment with id=1 is not linked to slug how-to-train-your-dragon-4\"}")
      )
    )

  def checkIfEmptyFieldsErrorOccurInCreate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      body: String
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callAddComment(authorizationHeader, uri, body)
    )(
      isLeft(
        equalTo(
          HttpError(
            """{"errors":{"body":["Invalid value for: body (expected comment.body to have length greater than or equal to 1, but got: \"\")"]}}""",
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def createAndCheckComment(
      authorizationHeader: Map[String, String],
      uri: Uri,
      body: String
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    for {
      comment <- callAddComment(authorizationHeader, uri, body)
    } yield zio.test.assert(comment.toOption) {
      isSome(
        hasField(
          "comment",
          _.comment,
          (hasField("body", _.body, equalTo("Amazing article!")): Assertion[Comment]) &&
            hasField(
              "author",
              _.author,
              (hasField("username", _.username.value, equalTo("michael")): Assertion[CommentAuthor]) && hasField(
                "following",
                _.following,
                isFalse
              )
            )
        )
      )
    }

  def checkCommentsList(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    for {
      commentsList <- callGetCommentsFromArticle(authorizationHeaderOpt, uri)
    } yield zio.test.assert(commentsList.toOption) {
      isSome(
        hasField(
          "comments",
          _.comments,
          hasSize(equalTo(2)) &&
            exists(
              (hasField("body", _.body, equalTo("Thank you so much!")): Assertion[Comment]) &&
                hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username.value, equalTo("jake")): Assertion[CommentAuthor]) && hasField(
                    "following",
                    _.following,
                    if (authorizationHeaderOpt.isDefined) isTrue else isFalse
                  )
                )
            ) &&
            exists(
              (hasField("body", _.body, equalTo("Great article!")): Assertion[Comment]) &&
                hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username.value, equalTo("michael")): Assertion[CommentAuthor]) && hasField(
                    "following",
                    _.following,
                    isFalse
                  )
                )
            )
        )
      )
    }

  def checkCommentsListAfterDelete(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[CommentsServerEndpoints, Throwable, TestResult] =
    for {
      commentsList <- callGetCommentsFromArticle(authorizationHeaderOpt, uri)
    } yield zio.test.assert(commentsList.toOption) {
      isSome(
        hasField(
          "comments",
          _.comments,
          hasSize(equalTo(1)) &&
            exists(
              (hasField("body", _.body, equalTo("Not bad.")): Assertion[Comment]) &&
                hasField(
                  "author",
                  _.author,
                  (hasField("username", _.username.value, equalTo("michael")): Assertion[CommentAuthor]) && hasField(
                    "following",
                    _.following,
                    isFalse
                  )
                )
            )
        )
      )
    }
