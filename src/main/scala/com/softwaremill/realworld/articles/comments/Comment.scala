package com.softwaremill.realworld.articles.comments

import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class CommentId(value: Int) extends AnyVal
case class Comment(id: CommentId, createdAt: Instant, updatedAt: Instant, body: String, author: CommentAuthor)

object Comment:
  given commentDataEncoder: JsonEncoder[Comment] = DeriveJsonEncoder.gen
  given commentDataDecoder: JsonDecoder[Comment] = DeriveJsonDecoder.gen

object CommentId:
  given commentIdEncoder: JsonEncoder[CommentId] = JsonEncoder[Int].contramap((c: CommentId) => c.value)
  given commentIdDecoder: JsonDecoder[CommentId] = JsonDecoder[Int].map(id => CommentId(id))
  given commentIdSchema: Schema[CommentId] = Schema(SchemaType.SInteger())
