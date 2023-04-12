package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.CommentsEndpointsSpec.suite
import com.softwaremill.realworld.articles.CommentsSpecData.*
import com.softwaremill.realworld.articles.comments.CommentCreateData
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.profiles.{ProfilesRepository, ProfilesService}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.TestUtils.{getValidAuthorizationHeader, testDbLayerWithFixture}
import sttp.client3.UriContext
import zio.test.TestServices.test
import zio.test.ZIOSpecDefault

object CommentsEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("check comments endpoints")(
    suite("with auth header")(
      suite("with auth data only")(
        test("return empty list if there are no comments for article") {
          for {
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfCommentsListIsEmpty(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2/comments"
            )
          } yield result
        },
        test("return non empty comment list") {
          for {
            authHeader <- getValidAuthorizationHeader(email = "john@example.com")
            result <- checkCommentsList(
              authorizationHeaderOpt = Some(authHeader),
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-3/comments"
            )
          } yield result
        },
        // TODO remove comment possible to check?

//        test("remove comment and check if comment list has one element") {
//          for {
//            authHeader <- getValidAuthorizationHeader(email = "john@example.com")
//            _ <- deleteComment(
//              authorizationHeader = authHeader,
//              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-4/comments/3"
//            )
//            result <- checkCommentsListAfterDelete(
//              authorizationHeaderOpt = Some(authHeader),
//              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-4/comments"
//            )
//          } yield result
//        },
        test("positive comment creation") {
          for {
            authHeader <- getValidAuthorizationHeader(email = "john@example.com")
            result <- createAndCheckComment(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/articles/how-to-train-your-dragon-6/comments",
              body = "Amazing article!"
            )
          } yield result
        }
      )
    ),
    suite("with no header")(
      test("return empty list if there are no comments for article") {
        for {
          result <- checkIfCommentsListIsEmpty(
            authorizationHeaderOpt = None,
            uri = uri"http://test.com/api/articles/how-to-train-your-dragon-2/comments"
          )
        } yield result
      },
      test("return non empty comment list") {
        for {
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
    ProfilesService.live,
    ProfilesRepository.live,
    testDbLayerWithFixture("fixtures/articles/comment-data.sql")
  )
