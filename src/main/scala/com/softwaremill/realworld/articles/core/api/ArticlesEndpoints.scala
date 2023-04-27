package com.softwaremill.realworld.articles.core.api

import com.softwaremill.realworld.articles.core.ArticlesFilters
import com.softwaremill.realworld.common.{BaseEndpoints, Pagination}
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

class ArticlesEndpoints(base: BaseEndpoints):

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

  val listArticlesEndpoint = base.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(articlesFilters)
    .in(articlesPagination)
    .out(jsonBody[ArticlesListResponse])

  val feedArticlesEndpoint = base.secureEndpoint.get
    .in("api" / "articles" / "feed")
    .in(articlesPagination)
    .out(jsonBody[ArticlesListResponse])

  val getEndpoint = base.secureEndpoint.get
    .in("api" / "articles" / path[String]("slug")) // TODO Input Validation
    .out(jsonBody[ArticleResponse])

  val createEndpoint = base.secureEndpoint.post
    .in("api" / "articles")
    .in(jsonBody[ArticleCreateRequest]) // TODO Input Validation
    .out(jsonBody[ArticleResponse])

  val deleteEndpoint = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug"))

  val updateEndpoint = base.secureEndpoint.put
    .in("api" / "articles" / path[String]("slug"))
    .in(jsonBody[ArticleUpdateRequest]) // TODO Input Validation
    .out(jsonBody[ArticleResponse])

  val makeFavoriteEndpoint = base.secureEndpoint.post
    .in("api" / "articles" / path[String]("slug") / "favorite")
    .out(jsonBody[ArticleResponse])

  val removeFavoriteEndpoint = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug") / "favorite")
    .out(jsonBody[ArticleResponse])

object ArticlesEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, ArticlesEndpoints] =
    ZLayer.fromFunction(new ArticlesEndpoints(_))
