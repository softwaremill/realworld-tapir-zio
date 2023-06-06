package com.softwaremill.realworld.articles.core.api

import com.softwaremill.realworld.articles.core.{Article, ArticleAuthor, ArticlesFilters}
import com.softwaremill.realworld.common.{BaseEndpoints, ErrorInfo, Pagination, UserSession}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import sttp.tapir.{EndpointInput, Validator}
import zio.ZLayer

import java.time.Instant
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

  val listArticlesEndpoint: ZPartialServerEndpoint[Any, Option[String], Option[
    UserSession
  ], (ArticlesFilters, Pagination), ErrorInfo, ArticlesListResponse, Any] = base.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(articlesFilters)
    .in(articlesPagination)
    .out(jsonBody[ArticlesListResponse].example(Examples.articlesListResponse))

  val feedArticlesEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Pagination, ErrorInfo, ArticlesListResponse, Any] =
    base.secureEndpoint.get
      .in("api" / "articles" / "feed")
      .in(articlesPagination)
      .out(jsonBody[ArticlesListResponse].example(Examples.articlesFeedResponse))

  val getEndpoint: ZPartialServerEndpoint[Any, String, UserSession, String, ErrorInfo, ArticleResponse, Any] = base.secureEndpoint.get
    .in("api" / "articles" / path[String]("slug"))
    .out(jsonBody[ArticleResponse].example(Examples.articleResponse))

  val createEndpoint: ZPartialServerEndpoint[Any, String, UserSession, ArticleCreateRequest, ErrorInfo, ArticleResponse, Any] =
    base.secureEndpoint.post
      .in("api" / "articles")
      .in(jsonBody[ArticleCreateRequest].example(Examples.articleCreateRequest))
      .out(jsonBody[ArticleResponse].example(Examples.articleResponse))

  val deleteEndpoint: ZPartialServerEndpoint[Any, String, UserSession, String, ErrorInfo, Unit, Any] = base.secureEndpoint.delete
    .in("api" / "articles" / path[String]("slug"))

  val updateEndpoint: ZPartialServerEndpoint[Any, String, UserSession, (String, ArticleUpdateRequest), ErrorInfo, ArticleResponse, Any] =
    base.secureEndpoint.put
      .in("api" / "articles" / path[String]("slug"))
      .in(jsonBody[ArticleUpdateRequest].example(Examples.articleUpdateRequest))
      .out(jsonBody[ArticleResponse].example(Examples.articleResponse))

  val makeFavoriteEndpoint: ZPartialServerEndpoint[Any, String, UserSession, String, ErrorInfo, ArticleResponse, Any] =
    base.secureEndpoint.post
      .in("api" / "articles" / path[String]("slug") / "favorite")
      .out(jsonBody[ArticleResponse].example(Examples.favoriteArticleResponse))

  val removeFavoriteEndpoint: ZPartialServerEndpoint[Any, String, UserSession, String, ErrorInfo, ArticleResponse, Any] =
    base.secureEndpoint.delete
      .in("api" / "articles" / path[String]("slug") / "favorite")
      .out(jsonBody[ArticleResponse].example(Examples.articleResponse))

  private object Examples:

    private val article1: Article = Article(
      slug = "how-to-train-your-dragon",
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "It takes a Jacobian",
      tagList = List("dragons", "training"),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      favorited = false,
      favoritesCount = 2,
      author = ArticleAuthor(username = "user1", bio = Some("user1Bio"), image = Some("user1Image"), following = false)
    )

    private val article2: Article = Article(
      slug = "how-to-train-your-dragon-2",
      title = "How to train your dragon 2",
      description = "So toothless",
      body = "Its a dragon",
      tagList = List("dragons", "goats", "training"),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      favorited = false,
      favoritesCount = 1,
      author = ArticleAuthor(username = "user1", bio = Some("user1Bio"), image = Some("user1Image"), following = false)
    )

    private val article3: Article = Article(
      slug = "how-to-train-your-dragon-3",
      title = "How to train your dragon 3",
      description = "So toothless",
      body = "Its a dragon",
      tagList = List(),
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      favorited = true,
      favoritesCount = 0,
      author = ArticleAuthor(username = "user2", bio = Some("user2Bio"), image = Some("user2Image"), following = false)
    )

    val articleResponse: ArticleResponse = ArticleResponse(article = article1)
    val favoriteArticleResponse: ArticleResponse = ArticleResponse(article = article3)
    val articlesFeedResponse: ArticlesListResponse = ArticlesListResponse(articles = List(article1, article2), articlesCount = 2)
    val articlesListResponse: ArticlesListResponse = ArticlesListResponse(articles = List(article1, article2, article3), articlesCount = 3)

    val articleCreateRequest: ArticleCreateRequest = ArticleCreateRequest(article =
      ArticleCreateData(
        title = "How to train your dragon",
        description = "Ever wonder how?",
        body = "It takes a Jacobian",
        tagList = Some(List("dragons", "training"))
      )
    )

    val articleUpdateRequest: ArticleUpdateRequest = ArticleUpdateRequest(article =
      ArticleUpdateData(
        title = Some("How to train your dragon"),
        description = Some("Ever wonder how?"),
        body = Some("It takes a Jacobian")
      )
    )

object ArticlesEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, ArticlesEndpoints] =
    ZLayer.fromFunction(new ArticlesEndpoints(_))
