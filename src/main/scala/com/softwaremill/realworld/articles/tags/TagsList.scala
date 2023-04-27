package com.softwaremill.realworld.articles.tags

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class TagsList(tags: List[String])

object TagsList:
  given tagsEncoder: JsonEncoder[TagsList] = DeriveJsonEncoder.gen[TagsList]
  given tagsDecoder: JsonDecoder[TagsList] = DeriveJsonDecoder.gen[TagsList]
