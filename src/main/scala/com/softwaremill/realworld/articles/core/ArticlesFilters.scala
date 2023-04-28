package com.softwaremill.realworld.articles.core

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
case class ArticlesFilters(
    tag: Option[String],
    author: Option[String],
    favorited: Option[String]
)
object ArticlesFilters:
  val empty: ArticlesFilters = ArticlesFilters(None, None, None)
  def withTag(tag: String): ArticlesFilters = ArticlesFilters(Some(tag), None, None)
  def withAuthor(author: String): ArticlesFilters = ArticlesFilters(None, Some(author), None)
  def withFavorited(favorited: String): ArticlesFilters = ArticlesFilters(None, None, Some(favorited))

  given articlesFiltersEncoder: JsonEncoder[ArticlesFilters] = DeriveJsonEncoder.gen[ArticlesFilters]
  given articlesFiltersDecoder: JsonDecoder[ArticlesFilters] = DeriveJsonDecoder.gen[ArticlesFilters]
