package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.comments.api.*
import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.common.ErrorMapper.defaultErrorsMappings
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class CommentsServerEndpoints(commentsService: CommentsService, commentsEndpoints: CommentsEndpoints):

  val addCommentServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.addCommentEndpoint
    .serverLogic(session =>
      case (slug, CommentCreateRequest(comment)) =>
        commentsService
          .addComment(slug, session.userId, comment.body)
          .pipe(defaultErrorsMappings)
          .map(CommentResponse.apply)
    )

  val deleteCommentServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.deleteCommentEndpoint
    .serverLogic(session =>
      case (slug, commentId) => commentsService.deleteComment(slug, session.userId, commentId).pipe(defaultErrorsMappings)
    )

  val getCommentsFromArticleServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.getCommentsFromArticleEndpoint
    .serverLogic(sessionOpt =>
      slug =>
        commentsService
          .getCommentsFromArticle(slug, sessionOpt.map(_.userId))
          .map(foundComments => CommentsListResponse(comments = foundComments))
          .pipe(defaultErrorsMappings)
    )

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    List(
      addCommentServerEndpoint,
      deleteCommentServerEndpoint,
      getCommentsFromArticleServerEndpoint
    )

object CommentsServerEndpoints:
  val live: ZLayer[CommentsService with CommentsEndpoints, Nothing, CommentsServerEndpoints] =
    ZLayer.fromFunction(new CommentsServerEndpoints(_, _))
