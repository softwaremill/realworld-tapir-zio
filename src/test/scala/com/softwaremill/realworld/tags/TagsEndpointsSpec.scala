package com.softwaremill.realworld.tags

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.{BaseEndpoints, Configuration}
import com.softwaremill.realworld.tags.TagsData.foundTags
import com.softwaremill.realworld.utils.TestUtils.*
import sttp.client3.ziojson.*
import sttp.client3.{ResponseException, UriContext, basicRequest}
import zio.test.Assertion.*
import zio.test.{ZIOSpecDefault, assertZIO}
import zio.ZIO

object TagsEndpointsSpec extends ZIOSpecDefault:

  def spec = suite("Tag endpoint tests")(
    test("return empty list if tags table is empty") {
      assertZIO(foundTags)(isRight(equalTo(TagsList(tags = List.empty[String]))))
    }.provide(
      Configuration.live,
      AuthService.live,
      BaseEndpoints.live,
      TagsRepository.live,
      TagsService.live,
      TagsEndpoints.live,
      testDbLayerWithEmptyDb
    ),
    test("return tags if tags table is non empty") {
      assertZIO(foundTags)(isRight(equalTo(TagsList(tags = List("dragons", "training", "dragons", "goats", "training")))))
    }.provide(
      Configuration.live,
      AuthService.live,
      BaseEndpoints.live,
      TagsRepository.live,
      TagsService.live,
      TagsEndpoints.live,
      testDbLayerWithFixture("fixtures/articles/basic-data.sql")
    )
  )

object TagsData {

  def foundTags: ZIO[TagsEndpoints, Throwable, Either[ResponseException[String, String], TagsList]] = {
    for {
      tagsEndpoints <- ZIO.service[TagsEndpoints]
      endpoint = tagsEndpoints.getTags
      response <- basicRequest
        .get(uri"http://test.com/api/tags")
        .response(asJson[TagsList])
        .send(backendStub(endpoint))
      body = response.body
    } yield body
  }
}
