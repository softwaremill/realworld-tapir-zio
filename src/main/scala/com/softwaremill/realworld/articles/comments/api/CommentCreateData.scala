package com.softwaremill.realworld.articles.comments.api

import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CommentCreateData(@validate(Validator.nonEmptyString) body: String)

object CommentCreateData:
  given commentCreateDataEncoder: JsonEncoder[CommentCreateData] = DeriveJsonEncoder.gen
  given commentCreateDataDecoder: JsonDecoder[CommentCreateData] = DeriveJsonDecoder.gen
