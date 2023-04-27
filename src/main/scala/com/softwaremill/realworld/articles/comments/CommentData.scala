package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.users.Profile
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class CommentData(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: Profile)

object CommentData:
  given commentDataEncoder: JsonEncoder[CommentData] = DeriveJsonEncoder.gen
  given commentDataDecoder: JsonDecoder[CommentData] = DeriveJsonDecoder.gen
