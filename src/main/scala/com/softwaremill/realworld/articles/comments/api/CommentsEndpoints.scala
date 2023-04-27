package com.softwaremill.realworld.articles.comments.api

import com.softwaremill.realworld.common.BaseEndpoints
import com.softwaremill.realworld.users.api.UsersEndpoints
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class CommentsEndpoints(base: BaseEndpoints):

  val addCommentEndpoint = base.secureEndpoint.post
    .in("api" / "articles" / path[String]("slug") / "comments")
    .in(jsonBody[CommentCreateRequest])
    .out(jsonBody[CommentResponse])

  val deleteCommentEndpoint = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug") / "comments" / path[Int]("id"))

  val getCommentsFromArticleEndpoint = base.optionallySecureEndpoint.get
    .in("api" / "articles" / path[String]("slug") / "comments")
    .out(jsonBody[CommentsListResponse])

object CommentsEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, CommentsEndpoints] = ZLayer.fromFunction(new CommentsEndpoints(_))
