package com.softwaremill.realworld.tags

import com.softwaremill.realworld.articles.ArticlesRepository
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.tags.TagEndpointTestSupport.*
import com.softwaremill.realworld.tags.TagRepositoryTestSupport.*
import com.softwaremill.realworld.utils.TestUtils.*
import com.softwaremill.realworld.users.UsersRepository
import sttp.client3.ziojson.*
import sttp.client3.{ResponseException, UriContext, basicRequest}
import zio.test.Assertion.*
import zio.test.{ZIOSpecDefault, assertZIO}
import zio.ZIO

object TagsEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("tag endpoint tests")(
    test("return empty list") {
      for {
        result <- checkIfTagsListIsEmpty(
          uri = uri"http://test.com/api/tags"
        )
      } yield result
    },
    test("return tags list") {
      for {
        _ <- prepareBasicTagsData
        result <- checkTags(
          uri = uri"http://test.com/api/tags"
        )
      } yield result
    }
  ).provide(
    Configuration.live,
    AuthService.live,
    BaseEndpoints.live,
    TagsRepository.live,
    TagsService.live,
    TagsEndpoints.live,
    ArticlesRepository.live,
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
