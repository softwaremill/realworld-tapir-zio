package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.*
import com.softwaremill.realworld.articles.comments.api.{CommentCreateRequest, CommentResponse, CommentsListResponse}
import com.softwaremill.realworld.articles.core.api.{ArticleCreateRequest, ArticleResponse, ArticleUpdateRequest, ArticlesListResponse}
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

class ArticlesEndpoints(articlesService: ArticlesService, base: BaseEndpoints):

  private def filterQuery(name: String): EndpointInput.Query[Option[String]] =
    query[Option[String]](name)
      .validateOption(Validator.nonEmptyString)
      .validateOption(Validator.maxLength(100))
      .validateOption(Validator.pattern("\\w+"))

  private val articlesFilters: EndpointInput[ArticlesFilters] =
    filterQuery("tag")
      .and(filterQuery("author"))
      .and(filterQuery("favorited"))
      .mapTo[ArticlesFilters]

  private val articlesPagination: EndpointInput[Pagination] =
    query[Int]("limit")
      .default(20)
      .validate(Validator.positive)
      .and(
        query[Int]("offset")
          .default(0)
          .validate(Validator.positiveOrZero)
      )
      .mapTo[Pagination]

  val listArticles: ZServerEndpoint[Any, Any] = base.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(articlesFilters)
    .in(articlesPagination)
    .out(jsonBody[ArticlesListResponse])
    .serverLogic(session =>
      (filters, pagination) =>
        articlesService
          .list(filters, pagination)
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val feedArticles: ZServerEndpoint[Any, Any] = base.secureEndpoint.get
    .in("api" / "articles" / "feed")
    .in(articlesPagination)
    .out(jsonBody[ArticlesListResponse])
    .serverLogic(session =>
      pagination =>
        articlesService
          .listArticlesByFollowedUsers(pagination, session.email)
          .map(articles => ArticlesListResponse(articles = articles, articlesCount = articles.size))
          .logError
          .pipe(defaultErrorsMappings)
    )

  val get: ZServerEndpoint[Any, Any] = base.secureEndpoint.get
    .in("api" / "articles" / path[String]("slug")) // TODO Input Validation
    .out(jsonBody[ArticleResponse])
    .serverLogic(session =>
      slug =>
        articlesService
          .findBySlugAsSeenBy(slug, session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val create: ZServerEndpoint[Any, Any] = base.secureEndpoint.post
    .in("api" / "articles")
    .in(jsonBody[ArticleCreateRequest]) // TODO Input Validation
    .out(jsonBody[ArticleResponse])
    .serverLogic(session =>
      data =>
        articlesService
          .create(data.article, session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val delete: ZServerEndpoint[Any, Any] = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug"))
    .serverLogic(session =>
      slug =>
        articlesService
          .delete(slug, session.email)
          .logError
          .pipe(defaultErrorsMappings)
    )

  val update: ZServerEndpoint[Any, Any] = base.secureEndpoint.put
    .in("api" / "articles" / path[String]("slug"))
    .in(jsonBody[ArticleUpdateRequest]) // TODO Input Validation
    .out(jsonBody[ArticleResponse])
    .serverLogic(session =>
      data =>
        articlesService
          .update(articleUpdateData = data._2.article, slug = data._1, email = session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val makeFavorite: ZServerEndpoint[Any, Any] = base.secureEndpoint.post
    .in("api" / "articles" / path[String]("slug") / "favorite")
    .out(jsonBody[ArticleResponse])
    .serverLogic(session =>
      slug =>
        articlesService
          .makeFavorite(slug, session.email)
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

  val removeFavorite: ZServerEndpoint[Any, Any] = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug") / "favorite")
    .out(jsonBody[ArticleResponse])
    .serverLogic(session =>
      slug =>
        articlesService
          .removeFavorite(slug, session.email)
          .pipe(defaultErrorsMappings)
          .map(ArticleResponse.apply)
    )

//  val addComment: ZServerEndpoint[Any, Any] = base.secureEndpoint.post
//    .in("api" / "articles" / path[String]("slug") / "comments")
//    .in(jsonBody[CommentCreateRequest])
//    .out(jsonBody[CommentResponse])
//    .serverLogic(session =>
//      case (slug, CommentCreateRequest(comment)) =>
//        articlesService
//          .addComment(slug, session.email, comment.body)
//          .pipe(defaultErrorsMappings)
//          .map(CommentResponse.apply)
//    )
//
//  val deleteComment: ZServerEndpoint[Any, Any] = base.secureEndpoint.delete
//    .in("api" / "articles" / path[String]("slug") / "comments" / path[Int]("id"))
//    .serverLogic(session =>
//      case (slug, commentId) => articlesService.deleteComment(slug, session.email, commentId).pipe(defaultErrorsMappings)
//    )
//
//  val getCommentsFromArticle: ZServerEndpoint[Any, Any] = base.optionallySecureEndpoint.get
//    .in("api" / "articles" / path[String]("slug") / "comments")
//    .out(jsonBody[CommentsListResponse])
//    .serverLogic(sessionOpt =>
//      slug =>
//        articlesService
//          .getCommentsFromArticle(slug, sessionOpt.map(_.email))
//          .map(foundComments => CommentsListResponse(comments = foundComments))
//          .pipe(defaultErrorsMappings)
//    )

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    List(
      listArticles,
      feedArticles,
      get,
      update,
      create,
      delete,
      makeFavorite,
      removeFavorite
    )

object ArticlesEndpoints:
  val live: ZLayer[ArticlesService with BaseEndpoints, Nothing, ArticlesEndpoints] = ZLayer.fromFunction(new ArticlesEndpoints(_, _))
