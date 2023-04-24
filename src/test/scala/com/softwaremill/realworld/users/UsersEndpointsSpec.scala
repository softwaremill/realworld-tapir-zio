package com.softwaremill.realworld.users

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.users.UserEndpointTestSupport.*
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.ZIOSpecDefault

object UsersEndpointsSpec extends ZIOSpecDefault:
  def spec = suite("users endpoints tests")(
    suite("get current user")(
      test("return not found error") {
        for {
          authHeader <- getValidAuthorizationHeader("invalid_email@invalid.com")
          result <- checkIfUserNotExistsErrorOccur(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
        } yield result
      },
      test("return valid user") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkGetCurrentUser(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
        } yield result
      }
    ),
    suite("user register")(
      test("return already in use error") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkIfUserAlreadyExistsErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/users",
            userRegisterData = UserRegisterData(email = "admin@example.com", username = "user", password = "password")
          )
        } yield result
      },
      test("return registered user") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkRegisterUser(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/users",
            userRegisterData = UserRegisterData(email = "new_user@example.com", username = "user", password = "password")
          )
        } yield result
      }
    ),
    suite("user login")(
      test("return invalid credentials error") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkIfInvalidPasswordErrorOccur(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/users/login",
            userLoginData = UserLoginData(email = "admin@example.com", password = "invalid_password")
          )
        } yield result
      },
      test("return logged in user") {
        for {
          authHeader <- getValidAuthorizationHeader()
          result <- checkLoginUser(
            authorizationHeader = authHeader,
            uri = uri"http://test.com/api/users/login",
            userLoginData = UserLoginData(email = "admin@example.com", password = "password")
          )
        } yield result
      }
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    UsersRepository.live,
    UsersService.live,
    UsersEndpoints.live,
    BaseEndpoints.live,
    testDbLayerWithEmptyDb
  )
