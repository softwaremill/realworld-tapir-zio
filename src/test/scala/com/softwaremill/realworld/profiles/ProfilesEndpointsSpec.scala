package com.softwaremill.realworld.profiles

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.profiles.ProfileDbTestSupport.*
import com.softwaremill.realworld.profiles.ProfileEndpointTestSupport.*
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.*
import sttp.model.StatusCode
import zio.test.*

object ProfilesEndpointsSpec extends ZIOSpecDefault:

  override def spec = suite("profile endpoints")(
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
          authHeader <- getValidAuthorizationHeader("jake@example.com")
          result <- checkFollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/john/follow")
        } yield result
      },
      test("unfollow profile") {
        for {
          _ <- prepareBasicProfileData
          authHeader <- getValidAuthorizationHeader("john@example.com")
          result <- checkUnfollowUser(authorizationHeaderOpt = Some(authHeader), uri = uri"http://test.com/api/profiles/jake/follow")
        } yield result
      }
    )
  ).provide(
    AuthService.live,
    BaseEndpoints.live,
    Configuration.live,
    ProfilesEndpoints.live,
    ProfilesRepository.live,
    ProfilesService.live,
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
