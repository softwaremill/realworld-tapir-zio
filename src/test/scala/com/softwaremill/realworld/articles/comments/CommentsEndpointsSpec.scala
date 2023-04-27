package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.comments.CommentCreateData
import com.softwaremill.realworld.articles.comments.CommentDbTestSupport.*
import com.softwaremill.realworld.articles.comments.CommentTestSupport.*
import com.softwaremill.realworld.articles.core.{ArticlesEndpoints, ArticlesRepository, ArticlesService}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.users.{UsersRepository, UsersService}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.TestServices.test
import zio.test.ZIOSpecDefault

object CommentsEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("comment endpoints endpoints")(
    suite("with auth header")(
      test("return empty list if there are no comments for article") {
        for {
          _ <- prepareDataForCommentsNotFound
          authHeader <- getValidAuthorizationHeader()
          result <- checkIfCommentsListIsEmpty(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2/comments"
          )
        } yield result
      },
      test("return non empty comments list") {
        for {
          _ <- prepareDataForCommentsList
          authHeader <- getValidAuthorizationHeader(email = "john@example.com")
          result <- checkCommentsList(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-3/comments"
          )
        } yield result
      },
      test("positive comment removing") {
        for {
          _ <- prepareDataForCommentsRemoving
          authHeader <- getValidAuthorizationHeader(email = "bill@example.com")
          _ <- deleteCommentRequest(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-4/comments/1"
          )
          result <- checkCommentsListAfterDelete(
            authorizationHeaderOpt = Some(authHeader),
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-4/comments"
          )
        } yield result
      },
      test("positive comment creation") {
        for {
          _ <- prepareDataForCommentsCreation
          authHeader <- getValidAuthorizationHeader(email = "michael@example.com")
          result <- createAndCheckComment(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-6/comments",
            body = "Amazing article!"
          )
        } yield result
      }
    ),
    suite("with no header")(
      test("return empty list if there are no comments for article") {
        for {
          _ <- prepareDataForCommentsNotFound
          result <- checkIfCommentsListIsEmpty(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2/comments"
          )
        } yield result
      },
      test("return non empty comments list") {
        for {
          _ <- prepareDataForCommentsList
          result <- checkCommentsList(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-3/comments"
          )
        } yield result
      }
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    UsersRepository.live,
    ArticlesRepository.live,
    ArticlesService.live,
    ArticlesEndpoints.live,
    BaseEndpoints.live,
    TagsRepository.live,
    UsersService.live,
    testDbLayerWithEmptyDb
  )
