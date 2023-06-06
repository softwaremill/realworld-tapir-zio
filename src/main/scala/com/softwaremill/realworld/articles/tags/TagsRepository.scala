package com.softwaremill.realworld.articles.tags

import io.getquill.*
import io.getquill.jdbczio.*
import zio.{ZIO, ZLayer}

import java.sql.SQLException

case class TagRow(
    articleId: Int,
    tag: String
)

class TagsRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryTags = quote(querySchema[TagRow](entity = "tags_articles"))

  def listTags: ZIO[Any, SQLException, List[String]] = run(queryTags).map(_.map(_.tag))

object TagsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, TagsRepository] =
    ZLayer.fromFunction(new TagsRepository(_))
