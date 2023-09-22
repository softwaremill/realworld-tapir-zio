package com.softwaremill.realworld.users

import com.softwaremill.realworld.users
import com.softwaremill.realworld.users.api.*
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Request, ResponseException, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertZIO}

object UserEndpointTestSupport:

  def callGetCurrentUser(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], UserResponse]] =
    ZIO
      .service[UsersServerEndpoints]
      .map(_.getCurrentUserServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .headers(authorizationHeader)
          .response(asJson[UserResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callRegisterUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], UserResponse]] =
    ZIO
      .service[UsersServerEndpoints]
      .map(_.registerServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .headers(authorizationHeader)
          .body(UserRegisterRequest(userRegisterData))
          .response(asJson[UserResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callLoginUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], UserResponse]] =
    ZIO
      .service[UsersServerEndpoints]
      .map(_.loginServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .post(uri)
          .headers(authorizationHeader)
          .body(UserLoginRequest(userLoginData))
          .response(asJson[UserResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callUpdateUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], UserResponse]] =
    ZIO
      .service[UsersServerEndpoints]
      .map(_.updateServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .put(uri)
          .headers(authorizationHeader)
          .body(UserUpdateRequest(userUpdateData))
          .response(asJson[UserResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def callGetProfile(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], ProfileResponse]] =
    val getProfileEndpoint = ZIO
      .service[UsersServerEndpoints]
      .map(_.getProfileServerEndpoint)

    val requestWithUri = basicRequest
      .get(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, getProfileEndpoint)

  def callFollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], ProfileResponse]] =
    val followUserEndpoint = ZIO
      .service[UsersServerEndpoints]
      .map(_.followUserServerEndpoint)

    val requestWithUri: Request[Either[String, String], Any] = basicRequest
      .post(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, followUserEndpoint)

  def callUnfollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, Either[ResponseException[String, String], ProfileResponse]] =
    val unfollowUserEndpoint = ZIO
      .service[UsersServerEndpoints]
      .map(_.unfollowUserServerEndpoint)

    val requestWithUri = basicRequest
      .delete(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, unfollowUserEndpoint)

  def executeRequest(
      authorizationHeaderOpt: Option[Map[String, String]],
      requestWithUri: Request[Either[String, String], Any],
      endpoint: ZIO[UsersServerEndpoints, Nothing, ZServerEndpoint[Any, Any]]
  ) =
    endpoint
      .flatMap { endpoint =>
        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[ProfileResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfUserNotExistsErrorOccurInGet(
      authorizationHeader: Map[String, String],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetCurrentUser(authorizationHeader, uri)
    )(isLeft(equalTo(HttpError("{\"error\":\"User with email invalid_email@invalid.com doesn't exist\"}", sttp.model.StatusCode(404)))))

  def checkIfUserEmailAlreadyExistsErrorOccurInRegister(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callRegisterUser(authorizationHeader, uri, userRegisterData)
    )(isLeft(equalTo(HttpError("{\"error\":\"User with email jake@example.com already in use\"}", sttp.model.StatusCode(409)))))

  def checkIfUserUsernameAlreadyExistsErrorOccurInRegister(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callRegisterUser(authorizationHeader, uri, userRegisterData)
    )(isLeft(equalTo(HttpError("{\"error\":\"User with username jake already in use\"}", sttp.model.StatusCode(409)))))

  def checkIfUserEmailAlreadyExistsErrorOccurInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateUser(authorizationHeader, uri, userUpdateData)
    )(isLeft(equalTo(HttpError("{\"error\":\"User with email john@example.com already in use\"}", sttp.model.StatusCode(409)))))

  def checkIfUserUsernameAlreadyExistsErrorOccurInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateUser(authorizationHeader, uri, userUpdateData)
    )(isLeft(equalTo(HttpError("{\"error\":\"User with username john already in use\"}", sttp.model.StatusCode(409)))))

  def checkIfEmptyFieldsErrorOccurInRegister(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callRegisterUser(authorizationHeader, uri, userRegisterData)
    )(
      isLeft(
        equalTo(
          HttpError(
            """{"errors":{"body":["Invalid value for: body (
              |expected user.email to have length greater than or equal to 1, but got: \"\",
              | expected user.username to have length greater than or equal to 3, but got: \"\",
              | expected user.password to have length greater than or equal to 1, but got: \"\")"]}}""".stripMargin
              .replaceAll("\\r?\\n", ""),
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def checkIfEmptyFieldsErrorOccurInLogin(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callLoginUser(authorizationHeader, uri, userLoginData)
    )(
      isLeft(
        equalTo(
          HttpError(
            """{"errors":{"body":["Invalid value for: body (
              |expected user.email to have length greater than or equal to 1, but got: \"\",
              | expected user.password to have length greater than or equal to 1, but got: \"\")"]}}""".stripMargin
              .replaceAll("\\r?\\n", ""),
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def checkIfEmptyFieldsErrorOccurInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateUser(authorizationHeader, uri, userUpdateData)
    )(
      isLeft(
        equalTo(
          HttpError(
            """{"errors":{"body":["Invalid value for: body (
              |expected user.email to have length greater than or equal to 1, but got: \"\",
              | expected user.username to have length greater than or equal to 3, but got: \"\",
              | expected user.password to have length greater than or equal to 1, but got: \"\",
              | expected user.bio to have length greater than or equal to 1, but got: \"\",
              | expected user.image to have length greater than or equal to 1, but got: \"\")"]}}""".stripMargin.replaceAll("\\r?\\n", ""),
            sttp.model.StatusCode(422)
          )
        )
      )
    )

  def checkIfInvalidEmailErrorOccurInRegister(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callRegisterUser(authorizationHeader, uri, userRegisterData)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"error\":\"Email invalid_email.com is not valid\"}",
            sttp.model.StatusCode(400)
          )
        )
      )
    )

  def checkIfInvalidEmailErrorOccurInLogin(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callLoginUser(authorizationHeader, uri, userLoginData)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"error\":\"Email invalid_email.com is not valid\"}",
            sttp.model.StatusCode(400)
          )
        )
      )
    )

  def checkIfInvalidEmailErrorOccurInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateUser(authorizationHeader, uri, userUpdateData)
    )(
      isLeft(
        equalTo(
          HttpError(
            "{\"error\":\"Email invalid_email.com is not valid\"}",
            sttp.model.StatusCode(400)
          )
        )
      )
    )

  def checkIfInvalidPasswordErrorOccurInLogin(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callLoginUser(authorizationHeader, uri, userLoginData)
    )(isLeft(equalTo(HttpError("{\"error\":\"Invalid email or password!\"}", sttp.model.StatusCode(401)))))

  def checkIfUnauthorizedErrorOccurInGet(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callGetProfile(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )

  def checkIfUnauthorizedErrorOccurInFollow(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callFollowUser(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )

  def checkIfUnauthorizedErrorOccurInUnfollow(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callUnfollowUser(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )

  def checkIfUserNotChangeInUpdate(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userUpdateData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callUpdateUser(authorizationHeader, uri, userUpdateData)
    )(
      isRight(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    )

  def checkGetCurrentUser(authorizationHeader: Map[String, String], uri: Uri): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(
      callGetCurrentUser(authorizationHeader, uri)
    )(
      isRight(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    )

  def checkRegisterUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userRegisterData: UserRegisterData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    for {
      userOrError <- callRegisterUser(authorizationHeader, uri, userRegisterData)
    } yield zio.test.assert(userOrError.toOption) {
      isSome(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("new_user@example.com")): Assertion[User]) &&
            hasField("token", _.token, isSome) &&
            hasField("username", _.username.value, equalTo("user")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    }

  def checkLoginUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      userLoginData: UserLoginData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    for {
      userOrError <- callLoginUser(authorizationHeader, uri, userLoginData)
    } yield zio.test.assert(userOrError.toOption) {
      isSome(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isSome) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      )
    }

  def checkUpdateUser(
      authorizationHeader: Map[String, String],
      uri: Uri,
      updateUserData: UserUpdateData
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    for {
      userOrError <- callUpdateUser(authorizationHeader, uri, updateUserData)
    } yield zio.test.assert(userOrError.toOption) {
      isSome(
        hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("updateduser@email.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username.value, equalTo("updatedUser")) &&
            hasField("bio", _.bio, equalTo(Some("updatedUserBio"))) &&
            hasField("image", _.image, equalTo(Some("updatedImageBio")))
        )
      )
    }

  def checkGetProfile(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callGetProfile(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username.value, equalTo("jake")): Assertion[Profile])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isFalse)
        )
      )
    )

  def checkFollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callFollowUser(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username.value, equalTo("john")): Assertion[Profile])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isTrue)
        )
      )
    )

  def checkUnfollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[UsersServerEndpoints, Throwable, TestResult] =
    assertZIO(callUnfollowUser(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username.value, equalTo("jake")): Assertion[Profile])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isFalse)
        )
      )
    )
