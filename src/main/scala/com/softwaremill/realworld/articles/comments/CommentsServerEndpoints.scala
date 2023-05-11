package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.comments.api.*
import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.common.ErrorMapper.defaultErrorsMappings
import com.softwaremill.realworld.db.{Db, DbConfig}
import io.getquill.SnakeCase
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.ztapir.*
import sttp.tapir.{EndpointInput, PublicEndpoint, Validator}
import zio.{Cause, Console, Exit, ZIO, ZLayer}

import javax.sql.DataSource
import scala.util.chaining.*

class CommentsServerEndpoints(commentsService: CommentsService, commentsEndpoints: CommentsEndpoints):

  val addCommentServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.addCommentEndpoint
    .serverLogic(session =>
      case (slug, CommentCreateRequest(comment)) =>
        commentsService
          .addComment(slug, session, comment.body)
          .pipe(defaultErrorsMappings)
          .map(CommentResponse.apply)
    )

  val deleteCommentServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.deleteCommentEndpoint
    .serverLogic(session =>
      case (slug, commentId) => commentsService.deleteComment(slug, session, commentId).pipe(defaultErrorsMappings)
    )

  val getCommentsFromArticleServerEndpoint: ZServerEndpoint[Any, Any] = commentsEndpoints.getCommentsFromArticleEndpoint
    .serverLogic(sessionOpt =>
      slug =>
        commentsService
          .getCommentsFromArticle(slug, sessionOpt)
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
