package com.softwaremill.realworld.articles.core.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ArticleCreateRequest(article: ArticleCreateData)

object ArticleCreateRequest:
  given articleCreateEncoder: JsonEncoder[ArticleCreateRequest] = DeriveJsonEncoder.gen[ArticleCreateRequest]
  given articleCreateDecoder: JsonDecoder[ArticleCreateRequest] = DeriveJsonDecoder.gen[ArticleCreateRequest]
