package com.softwaremill.realworld.articles.comments.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CommentCreateRequest(comment: CommentCreateData)

object CommentCreateRequest:
  given commentCreateEncoder: JsonEncoder[CommentCreateRequest] = DeriveJsonEncoder.gen
  given commentCreateDecoder: JsonDecoder[CommentCreateRequest] = DeriveJsonDecoder.gen
