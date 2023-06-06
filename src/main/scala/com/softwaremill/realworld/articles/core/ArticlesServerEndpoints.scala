package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.core.api.*
import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.common.ErrorMapper.defaultErrorsMappings
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class ArticlesServerEndpoints(articlesEndpoints: ArticlesEndpoints, articlesService: ArticlesService):

  val listArticlesServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.listArticlesEndpoint
    .serverLogic(sessionOpt =>
      (filters, pagination) =>
        articlesService
          .list(filters, pagination, sessionOpt.map(session => session.userId))
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val feedArticlesServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.feedArticlesEndpoint
    .serverLogic(session =>
      pagination =>
        articlesService
          .listArticlesByFollowedUsers(pagination, session.userId)
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val getServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.getEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .findBySlug(slug, session.userId)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val createServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.createEndpoint
    .serverLogic(session =>
      data =>
        articlesService
          .create(data.article, session.userId)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val deleteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.deleteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .delete(slug, session.userId)
          .logError
          .pipe(defaultErrorsMappings)
    )

  val updateServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.updateEndpoint
    .serverLogic(session =>
      data =>
        articlesService
          .update(articleUpdateData = data._2.article, slug = data._1, userId = session.userId)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val makeFavoriteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.makeFavoriteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .makeFavorite(slug, session.userId)
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val removeFavoriteServerEndpoint: ZServerEndpoint[Any, Any] = articlesEndpoints.removeFavoriteEndpoint
    .serverLogic(session =>
      slug =>
        articlesService
          .removeFavorite(slug, session.userId)
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
