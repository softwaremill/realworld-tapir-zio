package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.ArticlesRepository
import io.getquill.*
import io.getquill.jdbczio.*
import zio.{Task, ZLayer}

import java.time.Instant

case class CommentRow(commentId: Int, articleId: Int, createdAt: Instant, updatedAt: Instant, authorId: Int, body: String)

class CommentsRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryCommentArticle = quote(querySchema[CommentRow](entity = "comments_articles"))

  def addComment(articleId: Int, authorId: Int, comment: String): Task[Index] = {
    val now = Instant.now()
    run {
      queryCommentArticle
        .insert(
          _.articleId -> lift(articleId),
          _.createdAt -> lift(now),
          _.updatedAt -> lift(now),
          _.authorId -> lift(authorId),
          _.body -> lift(comment)
        )
        .returningGenerated(_.commentId)
    }
  }

  def deleteComment(commentId: Int): Task[Long] =
    run(queryCommentArticle.filter(_.commentId == lift(commentId)).delete)

  def deleteCommentsByArticleId(articleId: Int): Task[Long] =
    run(queryCommentArticle.filter(_.articleId == lift(articleId)).delete)

  def findComment(commentId: Int): Task[Option[CommentRow]] =
    run(queryCommentArticle.filter(_.commentId == lift(commentId)))
      .map(_.headOption)

  def findComments(articleId: Int): Task[List[CommentRow]] =
    run(queryCommentArticle.filter(_.articleId == lift(articleId)))

object CommentsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, CommentsRepository] =
    ZLayer.fromFunction(new CommentsRepository(_))
