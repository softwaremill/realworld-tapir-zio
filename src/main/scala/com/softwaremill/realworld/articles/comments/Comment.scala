package com.softwaremill.realworld.articles.comments

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class Comment(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: CommentAuthor)

object Comment:
  given commentDataEncoder: JsonEncoder[Comment] = DeriveJsonEncoder.gen
  given commentDataDecoder: JsonDecoder[Comment] = DeriveJsonDecoder.gen
