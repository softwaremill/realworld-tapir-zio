package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.{ArticleFavoriteRow, ArticleRow, ArticlesRepository, FollowerRow}
import io.getquill.*
import io.getquill.jdbczio.*
import zio.{Task, ZLayer}

import java.time.Instant

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
case class CommentRow(commentId: Int, articleId: Int, createdAt: Instant, updatedAt: Instant, authorId: Int, body: String)
case class FollowerRow(userId: Int, followerId: Int)
case class ProfileRow(userId: Int, username: String, bio: Option[String], image: Option[String])

class CommentsRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryArticle = quote(querySchema[ArticleRow](entity = "articles"))
  private inline def queryComment = quote(querySchema[CommentRow](entity = "comments_articles"))
  private inline def queryFollower = quote(querySchema[FollowerRow](entity = "followers"))
  private inline def queryProfile = quote(querySchema[ProfileRow](entity = "users"))

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
        pr <- queryProfile.join(pr => pr.userId == cr.authorId)
        ar <- queryArticle.join(ar => cr.articleId == ar.articleId)
      } yield (pr.userId, ar.articleId)
    )
      .map(_.headOption)

  def findComment(commentId: Int, viewerId: Int): Task[Option[Comment]] = {
    val commentRow: Quoted[Query[CommentRow]] = quote {
      queryComment.filter(cr => cr.commentId == lift(commentId))
    }

    val commentQuery = buildCommentQueryWithFollowing(commentRow, viewerId)

    run(commentQuery)
      .map(_.headOption)
      .map(_.map(comment))
  }

  def findComments(articleId: Int, viewerIdOpt: Option[Int]): Task[List[Comment]] = {
    val commentRow: Quoted[Query[CommentRow]] = quote {
      queryComment.filter(cr => cr.articleId == lift(articleId))
    }

    val commentQuery = viewerIdOpt match
      case Some(viewerId) => buildCommentQueryWithFollowing(commentRow, viewerId)
      case None           => buildCommentQuery(commentRow)

    run(commentQuery).map(_.map(comment))
  }

  private def comment(tuple: (CommentRow, ProfileRow, Boolean)): Comment = {
    val (cr, pr, isFollowing) = tuple

    Comment(
      id = cr.commentId,
      createdAt = cr.createdAt,
      updatedAt = cr.updatedAt,
      body = cr.body,
      author = CommentAuthor(
        username = pr.username,
        bio = pr.bio,
        image = pr.image,
        following = isFollowing
      )
    )
  }

  private def buildCommentQuery(crq: Quoted[Query[CommentRow]]) = {
    quote {
      for {
        cr <- crq
        pr <- queryProfile.join(ar => ar.userId == cr.authorId)
      } yield (cr, pr, false)
    }
  }

  private def buildCommentQueryWithFollowing(crq: Quoted[Query[CommentRow]], viewerId: Int) = {
    quote {
      for {
        tuple <- buildCommentQuery(crq)
        isFollowing = queryFollower
          .filter(f => (f.userId == tuple._2.userId) && (f.followerId == lift(viewerId)))
          .map(_ => 1)
          .nonEmpty
      } yield (tuple._1, tuple._2, isFollowing)
    }
  }

object CommentsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, CommentsRepository] =
    ZLayer.fromFunction(new CommentsRepository(_))
