package com.softwaremill.realworld.articles.model

import com.softwaremill.realworld.articles.model.ArticleCreateData
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ArticleCreateData(
    title: String,
    description: String,
    body: String,
    tagList: Option[List[String]]
)

object ArticleCreateData:
  given articleCreateDataEncoder: JsonEncoder[ArticleCreateData] = DeriveJsonEncoder.gen[ArticleCreateData]
  given articleCreateDataDecoder: JsonDecoder[ArticleCreateData] = DeriveJsonDecoder.gen[ArticleCreateData]
