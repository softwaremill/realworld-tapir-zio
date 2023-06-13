package com.softwaremill.realworld.articles.comments.api

import com.softwaremill.realworld.articles.comments.{Comment, CommentAuthor, CommentId}
import com.softwaremill.realworld.articles.core.ArticleSlug
import com.softwaremill.realworld.common.domain.Username
import com.softwaremill.realworld.common.{BaseEndpoints, ErrorInfo, UserSession}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZLayer

import java.time.Instant
import scala.util.chaining.*

class CommentsEndpoints(base: BaseEndpoints):

  val addCommentEndpoint
      : ZPartialServerEndpoint[Any, String, UserSession, (ArticleSlug, CommentCreateRequest), ErrorInfo, CommentResponse, Any] =
    base.secureEndpoint.post
      .in("api" / "articles" / path[ArticleSlug]("slug") / "comments")
      .in(jsonBody[CommentCreateRequest].example(Examples.commentCreateRequest))
      .out(jsonBody[CommentResponse].example(Examples.commentResponse))

  val deleteCommentEndpoint: ZPartialServerEndpoint[Any, String, UserSession, (ArticleSlug, CommentId), ErrorInfo, Unit, Any] =
    base.secureEndpoint.delete
      .in("api" / "articles" / path[ArticleSlug]("slug") / "comments" / path[CommentId]("id"))

  val getCommentsFromArticleEndpoint
      : ZPartialServerEndpoint[Any, Option[String], Option[UserSession], ArticleSlug, ErrorInfo, CommentsListResponse, Any] =
    base.optionallySecureEndpoint.get
      .in("api" / "articles" / path[ArticleSlug]("slug") / "comments")
      .out(jsonBody[CommentsListResponse].example(Examples.commentsListResponse))

  private object Examples:
    private val comment1: Comment = Comment(
      id = CommentId(1),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      body = "exampleComment1",
      author = CommentAuthor(username = Username("user1"), bio = Some("user1Bio"), image = Some("user1Image"), following = false)
    )

    private val comment2 = Comment(
      id = CommentId(2),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      body = "exampleComment2",
      author = CommentAuthor(username = Username("user2"), bio = Some("user2Bio"), image = Some("user2Image"), following = false)
    )

    val commentCreateRequest: CommentCreateRequest = CommentCreateRequest(comment = CommentCreateData(body = "exampleComment"))
    val commentResponse: CommentResponse = CommentResponse(comment = comment1)
    val commentsListResponse: CommentsListResponse = CommentsListResponse(comments = List(comment1, comment2))

object CommentsEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, CommentsEndpoints] = ZLayer.fromFunction(new CommentsEndpoints(_))
