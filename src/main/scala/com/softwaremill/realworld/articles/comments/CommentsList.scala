package com.softwaremill.realworld.articles.comments

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CommentsList(comments: List[CommentData])

object CommentsList:
  given commentsListEncoder: JsonEncoder[CommentsList] = DeriveJsonEncoder.gen[CommentsList]
  given commentsListDecoder: JsonDecoder[CommentsList] = DeriveJsonDecoder.gen[CommentsList]
