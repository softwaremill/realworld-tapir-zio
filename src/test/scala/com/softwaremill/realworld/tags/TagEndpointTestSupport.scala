package com.softwaremill.realworld.tags

import com.softwaremill.realworld.utils.TestUtils.backendStub
import sttp.client3.basicRequest
import sttp.client3.ziojson.asJson
import sttp.model.Uri
import zio.ZIO
import zio.test.{TestResult, assertZIO}
import zio.test.Assertion.{equalTo, isRight}

object TagEndpointTestSupport {

  def callGetTags(uri: Uri) =
    ZIO
      .service[TagsEndpoints]
      .map(_.getTags)
      .flatMap { endpoint =>
        basicRequest
          .get(uri)
          .response(asJson[TagsList])
          .send(backendStub(endpoint))
          .map(_.body)
      }

  def checkIfTagsListIsEmpty(
      uri: Uri
  ): ZIO[TagsEndpoints, Throwable, TestResult] = {

    assertZIO(callGetTags(uri))(
      isRight(equalTo(TagsList(tags = List.empty[String])))
    )
  }

  def checkTags(
      uri: Uri
  ): ZIO[TagsEndpoints, Throwable, TestResult] = {

    assertZIO(callGetTags(uri))(
      isRight(equalTo(TagsList(tags = List("dragons", "training", "dragons", "goats", "training"))))
    )
  }
}
