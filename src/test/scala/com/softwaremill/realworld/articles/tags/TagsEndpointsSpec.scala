package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.articles.core.ArticlesRepository
import com.softwaremill.realworld.articles.tags.TagDbTestSupport.*
import com.softwaremill.realworld.articles.tags.TagEndpointTestSupport.*
import com.softwaremill.realworld.articles.tags.api.TagsEndpoints
import com.softwaremill.realworld.articles.tags.{TagsRepository, TagsServerEndpoints, TagsService}
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.users.UsersRepository
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.UriContext
import zio.test.ZIOSpecDefault

object TagsEndpointsSpec extends ZIOSpecDefault:

  override def spec = suite("tag endpoints tests")(
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
    TagsServerEndpoints.live,
    ArticlesRepository.live,
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
