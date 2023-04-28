package com.softwaremill.realworld.articles.comments.api

import com.softwaremill.realworld.articles.comments.Comment
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CommentsListResponse(comments: List[Comment])

object CommentsListResponse:
  given commentsListEncoder: JsonEncoder[CommentsListResponse] = DeriveJsonEncoder.gen[CommentsListResponse]
  given commentsListDecoder: JsonDecoder[CommentsListResponse] = DeriveJsonDecoder.gen[CommentsListResponse]
