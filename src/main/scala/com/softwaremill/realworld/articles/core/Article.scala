package com.softwaremill.realworld.articles.core

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class Article(
    slug: String,
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
