package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.{ArticleRow, ArticlesRepository}
import io.getquill.*
import io.getquill.jdbczio.*
import zio.{Task, ZLayer}

import java.time.Instant

case class CommentRow(commentId: Int, articleId: Int, createdAt: Instant, updatedAt: Instant, authorId: Int, body: String)
case class ProfileRow(userId: Int, username: String, bio: Option[String], image: Option[String])
case class ArticleRow(
    articleId: Int,
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: Int
)

class CommentsRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryArticle = quote(querySchema[ArticleRow](entity = "articles"))
  private inline def queryProfile = quote(querySchema[ProfileRow](entity = "users"))
  private inline def queryComment = quote(querySchema[CommentRow](entity = "comments_articles"))

  def addComment(articleId: Int, authorId: Int, comment: String): Task[Index] = {
    val now = Instant.now()
    run {
      queryComment
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
    run(queryComment.filter(_.commentId == lift(commentId)).delete)

  def deleteCommentsByArticleId(articleId: Int): Task[Long] =
    run(queryComment.filter(_.articleId == lift(articleId)).delete)

  def findArticleAndAuthorIdsFromComment(commentId: Int): Task[Option[(Int, Int)]] =
    run(
      for {
        cr <- queryComment if cr.commentId == lift(commentId)
        prOpt <- queryProfile.leftJoin(pr => pr.userId == cr.authorId)
        arOpt <- queryArticle.leftJoin(ar => cr.articleId == ar.articleId)
      } yield {
        for {
          prIdOpt <- prOpt.map(_.userId)
          arIdOpt <- arOpt.map(_.articleId)
        } yield (prIdOpt, arIdOpt)
      }
    )
      .map(_.flatten.headOption)

  def findComment(commentId: Int): Task[Option[Comment]] =
    run(
      for {
        cr <- queryComment if cr.commentId == lift(commentId)
        ar <- queryProfile.leftJoin(ar => ar.userId == cr.authorId)
      } yield (cr, ar)
    )
      .map(_.headOption)
      .map(_.flatMap(comment))

  def findComments(articleId: Int): Task[List[Comment]] =
    run(
      for {
        cr <- queryComment if cr.articleId == lift(articleId)
        ar <- queryProfile.leftJoin(ar => ar.userId == cr.authorId)
      } yield (cr, ar)
    )
      .map(x => x.flatMap(comment))

  private def comment(tuple: (CommentRow, Option[ProfileRow])): Option[Comment] = {
    val (cr, arOpt) = tuple

    arOpt.map(ar =>
      Comment(
        id = cr.commentId,
        createdAt = cr.createdAt,
        updatedAt = cr.updatedAt,
        body = cr.body,
        author = CommentAuthor(
          username = ar.username,
          bio = ar.bio,
          image = ar.image,
          // TODO implement "following"
          following = false
        )
      )
    )
  }

object CommentsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, CommentsRepository] =
    ZLayer.fromFunction(new CommentsRepository(_))
