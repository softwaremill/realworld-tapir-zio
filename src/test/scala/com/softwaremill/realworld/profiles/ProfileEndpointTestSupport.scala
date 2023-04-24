package com.softwaremill.realworld.profiles

import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.asJson
import sttp.client3.{HttpError, Request, ResponseException, basicRequest}
import sttp.model.{StatusCode, Uri}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO
import zio.test.Assertion.*
import zio.test.*

object ProfileEndpointTestSupport:

  def callGetProfile(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, Either[ResponseException[String, String], Profile]] =
    val getProfileEndpoint = ZIO
      .service[ProfilesEndpoints]
      .map(_.getProfile)

    val requestWithUri = basicRequest
      .get(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, getProfileEndpoint)

  def callFollowUser(authorizationHeaderOpt: Option[Map[String, String]], uri: Uri) =
    val followUserEndpoint = ZIO
      .service[ProfilesEndpoints]
      .map(_.followUser)

    val requestWithUri: Request[Either[String, String], Any] = basicRequest
      .post(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, followUserEndpoint)

  def callUnfollowUser(authorizationHeaderOpt: Option[Map[String, String]], uri: Uri) =
    val unfollowUserEndpoint = ZIO
      .service[ProfilesEndpoints]
      .map(_.unfollowUser)

    val requestWithUri = basicRequest
      .delete(uri)

    executeRequest(authorizationHeaderOpt, requestWithUri, unfollowUserEndpoint)

  def executeRequest(
      authorizationHeaderOpt: Option[Map[String, String]],
      requestWithUri: Request[Either[String, String], Any],
      endpoint: ZIO[ProfilesEndpoints, Nothing, ZServerEndpoint[Any, Any]]
  ) =
    endpoint
      .flatMap { endpoint =>
        val requestAfterAuthorization = authorizationHeaderOpt match
          case Some(authorizationHeader) => requestWithUri.headers(authorizationHeader)
          case None                      => requestWithUri

        requestAfterAuthorization
          .response(asJson[Profile])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfUnauthorizedErrorOccurInGet(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callGetProfile(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )
  }

  def checkIfUnauthorizedErrorOccurInFollow(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callFollowUser(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )
  }

  def checkIfUnauthorizedErrorOccurInUnfollow(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callUnfollowUser(authorizationHeaderOpt, uri))(
      isLeft(isSubtype[HttpError[_]](hasField("statusCode", _.statusCode, equalTo(StatusCode.Unauthorized))))
    )
  }

  def checkGetProfile(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callGetProfile(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username, equalTo("jake")): Assertion[ProfileData])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isTrue)
        )
      )
    )
  }

  def checkFollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callFollowUser(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username, equalTo("john")): Assertion[ProfileData])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isTrue)
        )
      )
    )
  }

  def checkUnfollowUser(
      authorizationHeaderOpt: Option[Map[String, String]],
      uri: Uri
  ): ZIO[ProfilesEndpoints, Throwable, TestResult] = {

    assertZIO(callUnfollowUser(authorizationHeaderOpt, uri))(
      isRight(
        hasField(
          "profile",
          _.profile,
          (hasField("username", _.username, equalTo("jake")): Assertion[ProfileData])
            && hasField("bio", _.bio, isNone)
            && hasField("image", _.image, isNone)
            && hasField("following", _.following, isFalse)
        )
      )
    )
  }
