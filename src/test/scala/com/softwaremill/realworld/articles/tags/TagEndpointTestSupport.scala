package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.articles.tags.TagsServerEndpoints
import com.softwaremill.realworld.articles.tags.api.TagsListResponse
import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.ziojson.asJson
import sttp.client3.{ResponseException, basicRequest}
import sttp.model.Uri
import zio.ZIO
import zio.test.Assertion.{equalTo, isRight}
import zio.test.{TestResult, assertZIO}

object TagEndpointTestSupport:

  def callGetTags(uri: Uri): ZIO[TagsServerEndpoints, Throwable, Either[ResponseException[String, String], TagsListResponse]] =
    ZIO
      .service[TagsServerEndpoints]
      .map(_.getTagsServerEndpoint)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .response(asJson[TagsListResponse])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfTagsListIsEmpty(
      uri: Uri
  ): ZIO[TagsServerEndpoints, Throwable, TestResult] = {

    assertZIO(callGetTags(uri))(
      isRight(equalTo(TagsListResponse(tags = List.empty[String])))
    )
  }

  def checkTags(
      uri: Uri
  ): ZIO[TagsServerEndpoints, Throwable, TestResult] = {

    assertZIO(callGetTags(uri))(
      isRight(equalTo(TagsListResponse(tags = List("dragons", "training", "dragons", "goats", "training"))))
    )
  }
