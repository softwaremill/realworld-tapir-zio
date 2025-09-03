package com.softwaremill.realworld.common

import com.softwaremill.realworld.articles.comments.api.{CommentResponse, CommentsEndpoints}
import com.softwaremill.realworld.articles.comments.{CommentsRepository, CommentsServerEndpoints, CommentsService}
import com.softwaremill.realworld.articles.core.api.{ArticleResponse, ArticlesEndpoints}
import com.softwaremill.realworld.articles.core.{ArticlesRepository, ArticlesServerEndpoints, ArticlesService}
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.model.auth.*
import com.softwaremill.realworld.users.api.{UserResponse, UsersEndpoints}
import com.softwaremill.realworld.users.{UsersRepository, UsersServerEndpoints, UsersService}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.ziojson.*
import sttp.client3.HttpError
import zio.test.Assertion.*
import zio.test.{Spec, ZIOSpecDefault, assertZIO}
import zio.ZLayer

object AuthorizationSpec extends ZIOSpecDefault:

  val userTestParameters: List[UserAuthTestParameters] = List(
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.getCurrentUser,
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.getCurrentUser,
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.update,
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.update,
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.getProfile("username"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.getProfile("username"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.followUser("username"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.followUser("username"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.unfollowUser("username"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    UserAuthTestParameters(
      endpointParam = UserAuthEndpointParameters.unfollowUser("username"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    )
  )
  val articleTestParameters: List[ArticleAuthTestParameters] = List(
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.listArticles,
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.feedArticles,
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.get("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.get("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.create,
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.create,
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.delete("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.delete("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.update("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.update("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.makeFavorite("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.makeFavorite("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.removeFavorite("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    ArticleAuthTestParameters(
      endpointParam = ArticleAuthEndpointParameters.removeFavorite("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    )
  )
  val commentTestParameters: List[CommentAuthTestParameters] = List(
    CommentAuthTestParameters(
      endpointParam = CommentAuthEndpointParameters.addComment("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    CommentAuthTestParameters(
      endpointParam = CommentAuthEndpointParameters.addComment("slug"),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    CommentAuthTestParameters(
      endpointParam = CommentAuthEndpointParameters.deleteComment("slug", 1),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    ),
    CommentAuthTestParameters(
      endpointParam = CommentAuthEndpointParameters.deleteComment("slug", 1),
      headers = Map(),
      expectedError = "Invalid value for: header Authorization (missing)"
    ),
    CommentAuthTestParameters(
      endpointParam = CommentAuthEndpointParameters.getCommentsFromArticle("slug"),
      headers = Map("Authorization" -> "Token Invalid JWT"),
      expectedError = "{\"error\":\"Invalid token!\"}"
    )
  )

  def userEndpointsAuthorizationTest(testParameters: UserAuthTestParameters): Spec[UsersServerEndpoints, Throwable] = {
    test(s"User endpoints negative authorization test [expected: ${testParameters.expectedError}]") {
      assertZIO(
        testParameters.endpoint
          .flatMap { endpoint =>
            testParameters.request
              .headers(testParameters.headers)
              .response(asJson[UserResponse])
              .send(backendStub(endpoint))
              .map(_.body)
          }
      )(isLeft(equalTo(HttpError(testParameters.expectedError, sttp.model.StatusCode(401)))))
    }
  }

  def articleEndpointsAuthorizationTest(testParameters: ArticleAuthTestParameters): Spec[ArticlesServerEndpoints, Throwable] = {
    test(s"Article endpoints negative authorization test [expected: ${testParameters.expectedError}]") {
      assertZIO(
        testParameters.endpoint
          .flatMap { endpoint =>
            testParameters.request
              .headers(testParameters.headers)
              .response(asJson[ArticleResponse])
              .send(backendStub(endpoint))
              .map(_.body)
          }
      )(isLeft(equalTo(HttpError(testParameters.expectedError, sttp.model.StatusCode(401)))))
    }
  }

  def commentEndpointsAuthorizationTest(testParameters: CommentAuthTestParameters): Spec[CommentsServerEndpoints, Throwable] = {
    test(s"Comment endpoints negative authorization test [expected: ${testParameters.expectedError}]") {
      assertZIO(
        testParameters.endpoint
          .flatMap { endpoint =>
            testParameters.request
              .headers(testParameters.headers)
              .response(asJson[CommentResponse])
              .send(backendStub(endpoint))
              .map(_.body)
          }
      )(isLeft(equalTo(HttpError(testParameters.expectedError, sttp.model.StatusCode(401)))))
    }
  }

  override def spec = suite("check authorization")(
    suite("articles endpoints")(
      articleTestParameters.map(articleEndpointsAuthorizationTest)*
    ),
    suite("user endpoints")(
      userTestParameters.map(userEndpointsAuthorizationTest)*
    ),
    suite("user endpoints")(
      commentTestParameters.map(commentEndpointsAuthorizationTest)*
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    UsersRepository.live,
    UsersService.live,
    UsersEndpoints.live,
    UsersServerEndpoints.live,
    ArticlesRepository.live,
    ArticlesService.live,
    ArticlesEndpoints.live,
    ArticlesServerEndpoints.live,
    CommentsRepository.live,
    CommentsService.live,
    CommentsEndpoints.live,
    CommentsServerEndpoints.live,
    BaseEndpoints.live,
    testDbLayer
  )
