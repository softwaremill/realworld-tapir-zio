package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.common.domain.Username
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class CommentAuthor(username: Username, bio: Option[String], image: Option[String], following: Boolean)

object CommentAuthor:
  given articleAuthorEncoder: JsonEncoder[CommentAuthor] = DeriveJsonEncoder.gen[CommentAuthor]
  given articleAuthorDecoder: JsonDecoder[CommentAuthor] = DeriveJsonDecoder.gen[CommentAuthor]
