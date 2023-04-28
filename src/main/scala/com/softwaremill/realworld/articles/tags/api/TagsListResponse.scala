package com.softwaremill.realworld.articles.tags.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class TagsListResponse(tags: List[String])

object TagsListResponse:
  given tagsEncoder: JsonEncoder[TagsListResponse] = DeriveJsonEncoder.gen[TagsListResponse]
  given tagsDecoder: JsonDecoder[TagsListResponse] = DeriveJsonDecoder.gen[TagsListResponse]
