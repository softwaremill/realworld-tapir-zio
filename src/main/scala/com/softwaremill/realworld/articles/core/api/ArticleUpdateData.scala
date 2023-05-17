package com.softwaremill.realworld.articles.core.api

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import sttp.tapir.Schema.annotations.validateEach
import sttp.tapir.Validator
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ArticleUpdateData(
    @validateEach(Validator.nonEmptyString) title: Option[String],
    @validateEach(Validator.nonEmptyString) description: Option[String],
    @validateEach(Validator.nonEmptyString) body: Option[String]
)

object ArticleUpdateData:
  given articleUpdateDataEncoder: JsonEncoder[ArticleUpdateData] = DeriveJsonEncoder.gen[ArticleUpdateData]
  given articleUpdateDataDecoder: JsonDecoder[ArticleUpdateData] = DeriveJsonDecoder.gen[ArticleUpdateData]
