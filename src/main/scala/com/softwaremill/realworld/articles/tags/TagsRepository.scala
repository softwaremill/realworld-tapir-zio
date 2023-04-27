package com.softwaremill.realworld.articles.tags

import io.getquill.*
import io.getquill.jdbczio.*
import zio.{Task, ZIO, ZLayer}

import java.sql.SQLException

class TagsRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryTags = quote(querySchema[TagRow](entity = "tags_articles"))

  def listTags: ZIO[Any, SQLException, List[TagRow]] = run(queryTags)

  def deleteTagsByArticleId(articleId: Int): Task[Long] =
    run(queryTags.filter(_.articleId == lift(articleId)).delete)

object TagsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, TagsRepository] =
    ZLayer.fromFunction(new TagsRepository(_))
