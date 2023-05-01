package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.*
import com.softwaremill.realworld.articles.comments.api.{CommentCreateRequest, CommentResponse, CommentsListResponse}
import com.softwaremill.realworld.articles.core.api.*
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
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}
import zio.{Cause, Console, Exit, IO, ZIO, ZLayer}

import javax.sql.DataSource
import scala.util.chaining.*

class ArticlesServerEndpoints(articlesEndpoints: ArticlesEndpoints, articlesService: ArticlesService):

  val listArticlesServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.listArticlesEndpoint
    .serverLogic(_ =>
      (filters, pagination) =>
        articlesService
          .list(filters, pagination)
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val feedArticlesServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.feedArticlesEndpoint
    .serverLogic(session =>
      pagination =>
        articlesService
          .listArticlesByFollowedUsers(pagination, session.email)
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val getServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.getEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .findBySlugAsSeenBy(slug, session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val createServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.createEndpoint
    .serverLogic(session =>
      data =>
        articlesService
          .create(data.article, session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val deleteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.deleteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .delete(slug, session.email)
          .logError
          .pipe(defaultErrorsMappings)
    )

  val updateServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.updateEndpoint
    .serverLogic(session =>
      data =>
        articlesService
          .update(articleUpdateData = data._2.article, slug = data._1, email = session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val makeFavoriteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.makeFavoriteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .makeFavorite(slug, session.email)
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val removeFavoriteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.removeFavoriteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .removeFavorite(slug, session.email)
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    List(
      listArticlesServerEndpoint,
      feedArticlesServerEndpoint,
      getServerEndpoint,
      updateServerEndpoint,
      createServerEndpoint,
      deleteServerEndpoint,
      makeFavoriteServerEndpoint,
      removeFavoriteServerEndpoint
    )

object ArticlesServerEndpoints:
  val live: ZLayer[ArticlesEndpoints with ArticlesService, Nothing, ArticlesServerEndpoints] =
    ZLayer.fromFunction(new ArticlesServerEndpoints(_, _))
