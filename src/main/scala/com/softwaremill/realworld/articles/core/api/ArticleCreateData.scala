package com.softwaremill.realworld.articles.core.api

import sttp.tapir.Schema.annotations.{validate, validateEach}
import sttp.tapir.ValidationResult.{Invalid, Valid}
import sttp.tapir.{ValidationResult, Validator}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

def validateTagList(tagList: List[String]): ValidationResult =
  if (tagList.exists(_.isEmpty)) Invalid("each element in the tagList is expected to have a length greater than or equal to 1") else Valid

case class ArticleCreateData(
    @validate(Validator.nonEmptyString) title: String,
    @validate(Validator.nonEmptyString) description: String,
    @validate(Validator.nonEmptyString) body: String,
    @validateEach(Validator.custom(validateTagList)) tagList: Option[List[String]]
)

object ArticleCreateData:
  given articleCreateDataEncoder: JsonEncoder[ArticleCreateData] = DeriveJsonEncoder.gen[ArticleCreateData]
  given articleCreateDataDecoder: JsonDecoder[ArticleCreateData] = DeriveJsonDecoder.gen[ArticleCreateData]
