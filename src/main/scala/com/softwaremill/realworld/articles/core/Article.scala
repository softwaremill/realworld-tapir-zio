package com.softwaremill.realworld.articles.core

import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class ArticleSlug(value: String) extends AnyVal

case class Article(
    slug: ArticleSlug,
    title: String,
    description: String,
    body: String,
    tagList: List[String],
    createdAt: Instant,
    updatedAt: Instant,
    favorited: Boolean,
    favoritesCount: Int,
    author: ArticleAuthor
)

object Article:
  given articleDataEncoder: JsonEncoder[Article] = DeriveJsonEncoder.gen[Article]
  given articleDataDecoder: JsonDecoder[Article] = DeriveJsonDecoder.gen[Article]

object ArticleSlug:
  given articleSlugEncoder: JsonEncoder[ArticleSlug] = JsonEncoder[String].contramap((c: ArticleSlug) => c.value)
  given articleSlugDecoder: JsonDecoder[ArticleSlug] = JsonDecoder[String].map(id => ArticleSlug(id))
  given articleSlugSchema: Schema[ArticleSlug] = Schema(SchemaType.SString())
