package com.softwaremill.realworld.users

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.UserDbTestSupport.*
import com.softwaremill.realworld.users.UserEndpointTestSupport.*
import com.softwaremill.realworld.users.api.{UserLoginData, UserRegisterData, UserUpdateData, UsersEndpoints}
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.ZIOSpecDefault

object UsersEndpointsSpec extends ZIOSpecDefault:
  override def spec = suite("user endpoints tests")(
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
          _ <- prepareTwoUsers
          authHeader <- getValidAuthorizationHeader("john@example.com")
          result <- checkGetProfile(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/jake")
        } yield result
      },
      test("follow profile") {
        for {
          _ <- prepareTwoUsers
          authHeader <- getValidAuthorizationHeader()
          result <- checkFollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/john/follow")
        } yield result
      },
      test("unfollow profile") {
        for {
          _ <- prepareTwoUsers
          authHeader <- getValidAuthorizationHeader("john@example.com")
          result <- checkUnfollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/jake/follow")
        } yield result
      },
      suite("get current user")(
        test("return not found error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader("invalid_email@invalid.com")
            result <- checkIfUserNotExistsErrorOccurInGet(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
          } yield result
        },
        test("return valid user") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkGetCurrentUser(authorizationHeader = authHeader, uri = uri"http://test.com/api/user")
          } yield result
        }
      ),
      suite("user register")(
        test("return empty string fields error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInRegister(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users",
              userRegisterData = UserRegisterData(email = "", username = "", password = "")
            )
          } yield result
        },
        test("return not valid email error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfInvalidEmailErrorOccurInRegister(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users",
              userRegisterData = UserRegisterData(email = "invalid_email.com", username = "new_username", password = "password")
            )
          } yield result
        },
        test("return email already in use error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserEmailAlreadyExistsErrorOccurInRegister(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users",
              userRegisterData = UserRegisterData(email = "jake@example.com", username = "new_username", password = "password")
            )
          } yield result
        },
        test("return username already in use error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserUsernameAlreadyExistsErrorOccurInRegister(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users",
              userRegisterData = UserRegisterData(email = "new_email@example.com", username = "jake", password = "password")
            )
          } yield result
        },
        test("return registered user") {
          for {
            _ <- prepareOneUser
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
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInLogin(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "", password = "")
            )
          } yield result
        },
        test("return not valid email error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfInvalidEmailErrorOccurInLogin(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "invalid_email.com", password = "password")
            )
          } yield result
        },
        test("return invalid credentials error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfInvalidPasswordErrorOccurInLogin(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "jake@example.com", password = "invalid_password")
            )
          } yield result
        },
        test("return logged in user") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkLoginUser(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/users/login",
              userLoginData = UserLoginData(email = "jake@example.com", password = "password")
            )
          } yield result
        }
      ),
      suite("user update")(
        test("return empty string fields error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfEmptyFieldsErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              userUpdateData = UserUpdateData(
                email = Some(""),
                username = Some(""),
                password = Some(""),
                bio = Some(""),
                image = Some("")
              )
            )
          } yield result
        },
        test("return not valid email error") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfInvalidEmailErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              userUpdateData = UserUpdateData(
                email = Some("invalid_email.com"),
                username = None,
                password = None,
                bio = None,
                image = None
              )
            )
          } yield result
        },
        test("return email already in use error") {
          for {
            _ <- prepareTwoUsers
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserEmailAlreadyExistsErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              userUpdateData = UserUpdateData(
                email = Some("john@example.com"),
                username = None,
                password = None,
                bio = None,
                image = None
              )
            )
          } yield result
        },
        test("return username already in use error") {
          for {
            _ <- prepareTwoUsers
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserUsernameAlreadyExistsErrorOccurInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              userUpdateData = UserUpdateData(
                email = None,
                username = Some("john"),
                password = None,
                bio = None,
                image = None
              )
            )
          } yield result
        },
        test("return not changed user") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkIfUserNotChangeInUpdate(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              userUpdateData = UserUpdateData(
                email = None,
                username = None,
                password = None,
                bio = None,
                image = None
              )
            )
          } yield result
        },
        test("return logged in user") {
          for {
            _ <- prepareOneUser
            authHeader <- getValidAuthorizationHeader()
            result <- checkUpdateUser(
              authorizationHeader = authHeader,
              uri = uri"http://test.com/api/user",
              updateUserData = UserUpdateData(
                email = Some("updatedUser@email.com"),
                username = Some("updatedUser"),
                password = Some("new_password"),
                bio = Some("updatedUserBio"),
                image = Some("updatedImageBio")
              )
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
