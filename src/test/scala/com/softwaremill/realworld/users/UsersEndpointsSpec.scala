package com.softwaremill.realworld.users

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.users.UserDbTestSupport.*
import com.softwaremill.realworld.users.UserEndpointTestSupport.*
import com.softwaremill.realworld.users.api.{UserLoginData, UserRegisterData, UsersEndpoints}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.ZIOSpecDefault

object UsersEndpointsSpec extends ZIOSpecDefault:
  def spec = suite("user endpoints tests")(
    suite("with no header")(
      test("get profile") {
        checkIfUnauthorizedErrorOccurInGet(authorizationHeaderOpt = None, uri = uri"http://test.com/api/profiles/jake")
      },
      test("follow profile") {
        checkIfUnauthorizedErrorOccurInFollow(authorizationHeaderOpt = None, uri = uri"http://test.com/api/profiles/jake/follow")
      },
      test("unfollow profile") {
        checkIfUnauthorizedErrorOccurInUnfollow(authorizationHeaderOpt = None, uri = uri"http://test.com/api/profiles/john/follow")
      }
    ),
    suite("with auth header")(
      test("get profile") {
        for {
          _ <- prepareBasicProfileData
          authHeader <- getValidAuthorizationHeader("john@example.com")
          result <- checkGetProfile(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/jake")
        } yield result
      },
      test("follow profile") {
        for {
          _ <- prepareBasicProfileData
          authHeader <- getValidAuthorizationHeader()
          result <- checkFollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/john/follow")
        } yield result
      },
      test("unfollow profile") {
        for {
          _ <- prepareBasicProfileData
          authHeader <- getValidAuthorizationHeader("john@example.com")
          result <- checkUnfollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/jake/follow")
        } yield result
      },
      suite("get current user")(
        test("return not found error") {
          for {
            _ <- prepareBasicUsersData
            authHeader <- getValidAuthorizationHeader("invalid_email@invalid.com")
            result <- checkIfUserNotExistsErrorOccur(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
          } yield result
        },
        test("return valid user") {
          for {
            _ <- prepareBasicUsersData
            authHeader <- getValidAuthorizationHeader()
            result <- checkGetCurrentUser(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
          } yield result
        }
      ),
      suite("user register")(
        test("return already in use error") {
          for {
            _ <- prepareBasicUsersData
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserAlreadyExistsErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users",
              userRegisterData = UserRegisterData(email = "jake@example.com", username = "jake", password = "password")
            )
          } yield result
        },
        test("return registered user") {
          for {
            _ <- prepareBasicUsersData
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
            _ <- prepareBasicUsersData
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfInvalidPasswordErrorOccur(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "jake@example.com", password = "invalid_password")
            )
          } yield result
        },
        test("return logged in user") {
          for {
            _ <- prepareBasicUsersData
            authHeader <- getValidAuthorizationHeader()
            result <- checkLoginUser(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "jake@example.com", password = "password")
            )
          } yield result
        }
      )
    )
  ).provide(
    Configuration.live,
    AuthService.live,
    UsersRepository.live,
    UsersService.live,
    UsersEndpoints.live,
    UsersServerEndpoints.live,
    BaseEndpoints.live,
    testDbLayerWithEmptyDb
  )
