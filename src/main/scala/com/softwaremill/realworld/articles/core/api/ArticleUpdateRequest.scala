package com.softwaremill.realworld.articles.core.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ArticleUpdateRequest(article: ArticleUpdateData)

object ArticleUpdateRequest:
  given articleUpdateEncoder: JsonEncoder[ArticleUpdateRequest] = DeriveJsonEncoder.gen[ArticleUpdateRequest]
  given articleUpdateDecoder: JsonDecoder[ArticleUpdateRequest] = DeriveJsonDecoder.gen[ArticleUpdateRequest]
