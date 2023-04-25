package com.softwaremill.realworld.users

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.users
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, basicRequest}
import sttp.model.Uri
import sttp.tapir.server.stub.TapirStubInterpreter
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertTrue, assertZIO}
import zio.{RIO, Random, ZIO, ZLayer}

object UserEndpointTestSupport:

  def callGetCurrentUser(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[UsersEndpoints, Throwable, Either[ResponseException[String, String], User]] =
    ZIO
      .service[UsersEndpoints]
      .map(_.getCurrentUser)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .headers(authorizationHeader)
          .response(asJson[User])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callRegisterUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersEndpoints, Throwable, Either[ResponseException[String, String], User]] =
    ZIO
      .service[UsersEndpoints]
      .map(_.register)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .headers(authorizationHeader)
          .body(UserRegister(userRegisterData))
          .response(asJson[User])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callLoginUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersEndpoints, Throwable, Either[ResponseException[String, String], User]] =
    ZIO
      .service[UsersEndpoints]
      .map(_.login)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .headers(authorizationHeader)
          .body(UserLogin(userLoginData))
          .response(asJson[User])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfUserNotExistsErrorOccur(authorizationHeader: Map[String, String], uri: Uri): ZIO[UsersEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetCurrentUser(authorizationHeader, uri)
    )(isLeft(equalTo(HttpError("{\"error\":\"User doesn't exist.\"}", sttp.model.StatusCode(404)))))

  def checkIfUserAlreadyExistsErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersEndpoints, Throwable, TestResult] =
    assertZIO(
      callRegisterUser(authorizationHeader, uri, userRegisterData)
    )(isLeft(equalTo(HttpError("{\"error\":\"E-mail already in use!\"}", sttp.model.StatusCode(409)))))

  def checkIfInvalidPasswordErrorOccur(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersEndpoints, Throwable, TestResult] =
    assertZIO(
      callLoginUser(authorizationHeader, uri, userLoginData)
    )(isLeft(equalTo(HttpError("{\"error\":\"Invalid email or password!\"}", sttp.model.StatusCode(401)))))

  def checkGetCurrentUser(authorizationHeader: Map[String, String], uri: Uri): ZIO[UsersEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetCurrentUser(authorizationHeader, uri)
    )(
      isRight(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email, equalTo("jake@example.com")): Assertion[UserData]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    )

  def checkRegisterUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersEndpoints, Throwable, TestResult] =
    for {
      userOrError <- callRegisterUser(authorizationHeader, uri, userRegisterData)
    } yield zio.test.assert(userOrError.toOption) {
      isSome(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email, equalTo("new_user@example.com")): Assertion[UserData]) &&
            hasField("token", _.token, isSome) &&
            hasField("username", _.username, equalTo("user")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    }

  def checkLoginUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersEndpoints, Throwable, TestResult] =
    for {
      userOrError <- callLoginUser(authorizationHeader, uri, userLoginData)
    } yield zio.test.assert(userOrError.toOption) {
      isSome(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email, equalTo("jake@example.com")): Assertion[UserData]) &&
            hasField("token", _.token, isSome) &&
            hasField("username", _.username, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    }
