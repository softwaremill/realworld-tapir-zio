package com.softwaremill.realworld.articles.comments

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
case class CommentQueryBuildSupport(
    commentRow: CommentRow,
    profileRow: ProfileRow,
    isFollowing: Boolean
)

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

  private def comment(cs: CommentQueryBuildSupport): Comment =
    Comment(
      id = cs.commentRow.commentId,
      createdAt = cs.commentRow.createdAt,
      updatedAt = cs.commentRow.updatedAt,
      body = cs.commentRow.body,
      author = CommentAuthor(
        username = cs.profileRow.username,
        bio = cs.profileRow.bio,
        image = cs.profileRow.image,
        following = cs.isFollowing
      )
    )

  private def buildCommentQuery(crq: Quoted[Query[CommentRow]]) =
    quote {
      for {
        cr <- crq
        pr <- queryProfile.join(ar => ar.userId == cr.authorId)
      } yield CommentQueryBuildSupport(commentRow = cr, profileRow = pr, isFollowing = false)
    }

  private def buildCommentQueryWithFollowing(crq: Quoted[Query[CommentRow]], viewerId: Int) =
    quote {
      for {
        cs <- buildCommentQuery(crq)
        isFollowing = queryFollower
          .filter(f => (f.userId == cs.profileRow.userId) && (f.followerId == lift(viewerId)))
          .map(_ => 1)
          .nonEmpty
      } yield CommentQueryBuildSupport(cs.commentRow, cs.profileRow, isFollowing)
    }

object CommentsRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, CommentsRepository] =
    ZLayer.fromFunction(new CommentsRepository(_))
