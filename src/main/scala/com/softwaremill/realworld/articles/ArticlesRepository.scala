package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.ArticlesTags.{explodeTags, tagsConcat}
import com.softwaremill.realworld.articles.comments.CommentRow
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.profiles.ProfileRow
import com.softwaremill.realworld.users.UserRow
import io.getquill.*
import io.getquill.jdbczio.*
import org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
import org.sqlite.{SQLiteErrorCode, SQLiteException}
import zio.{Console, IO, RIO, Task, UIO, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource
import scala.collection.immutable
import scala.util.chaining.*

class ArticlesRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryArticle = quote(querySchema[ArticleRow](entity = "articles"))
  private inline def queryTagArticle = quote(querySchema[ArticleTagRow](entity = "tags_articles"))
  private inline def queryFavoriteArticle = quote(querySchema[ArticleFavoriteRow](entity = "favorites_articles"))
  private inline def queryProfile = quote(querySchema[ProfileRow](entity = "users"))
  private inline def queryUser = quote(querySchema[UserRow](entity = "users"))
  private inline def queryCommentArticle = quote(querySchema[CommentRow](entity = "comments_articles"))

  def list(filters: ArticlesFilters, pagination: Pagination): IO[SQLException, List[ArticleData]] = {
    val tagFilter = filters.tag.getOrElse("")
    val authorFilter = filters.author.getOrElse("")
    val favoritedFilter = filters.favorited.getOrElse("")

    run(for {
      ar <- sql"""
                     SELECT a.article_id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id
                     FROM articles a
                     LEFT JOIN users authors ON authors.user_id = a.author_id
                     LEFT JOIN favorites_articles fa ON fa.article_id = a.article_id
                     LEFT JOIN users fu ON fu.user_id = fa.profile_id
                     LEFT JOIN tags_articles ta ON a.article_id = ta.article_id
                     WHERE (${lift(tagFilter)} = '' OR ${lift(tagFilter)} = ta.tag)
                          AND (${lift(favoritedFilter)} = '' OR ${lift(favoritedFilter)} = fu.username)
                          AND (${lift(authorFilter)} = '' OR ${lift(authorFilter)} = authors.username)
                     GROUP BY a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id
                   """
        .as[Query[ArticleRow]]
        .drop(lift(pagination.offset))
        .take(lift(pagination.limit))
        .sortBy(ar => ar.slug)
      tr <- queryTagArticle
        .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
        .leftJoin(a => a._1 == ar.articleId)
      fr <- queryFavoriteArticle
        .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
        .leftJoin(f => f._1 == ar.articleId)
      pr <- queryProfile if ar.authorId == pr.userId
    } yield (ar, pr, tr.map(_._2), fr.map(_._2)))
      .map(x => x.map(article))
  }

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      followerId: Int
  ): IO[SQLException, List[ArticleData]] = {

    run(for {
      ar <- sql"""
                     SELECT DISTINCT * FROM articles a
                     WHERE a.author_id IN (SELECT f.user_id FROM followers f
                                           WHERE f.follower_id = ${lift(followerId)})
                   """
        .as[Query[ArticleRow]]
        .drop(lift(pagination.offset))
        .take(lift(pagination.limit))
        .sortBy(ar => ar.slug)
      tr <- queryTagArticle
        .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
        .leftJoin(a => a._1 == ar.articleId)
      fr <- queryFavoriteArticle
        .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
        .leftJoin(f => f._1 == ar.articleId)
      pr <- queryProfile if ar.authorId == pr.userId
    } yield (ar, pr, tr.map(_._2), fr.map(_._2)))
      .map(_.map(article))
  }

  def findBySlug(slug: String): IO[SQLException, Option[ArticleData]] =
    run(for {
      ar <- queryArticle if ar.slug == lift(slug)
      tr <- queryTagArticle
        .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
        .leftJoin(a => a._1 == ar.articleId)
      fr <- queryFavoriteArticle
        .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
        .leftJoin(f => f._1 == ar.articleId)
      pr <- queryProfile if ar.authorId == pr.userId
    } yield (ar, pr, tr.map(_._2), fr.map(_._2), false))
      .map(_.headOption)
      .map(_.map(mapToArticleData))

  def findArticleIdBySlug(slug: String): Task[Int] =
    run(
      queryArticle
        .filter(a => a.slug == lift(slug))
        .map(_.articleId)
    )
      .map(_.headOption)
      .flatMap {
        case Some(a) => ZIO.succeed(a)
        case None    => ZIO.fail(Exceptions.NotFound(s"Article with slug $slug doesn't exist."))
      }

  def findBySlugAsSeenBy(slug: String, viewerEmail: String): IO[SQLException, Option[ArticleData]] =
    run(for {
      ar <- queryArticle if ar.slug == lift(slug)
      tr <- queryTagArticle
        .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
        .leftJoin(a => a._1 == ar.articleId)
      fr <- queryFavoriteArticle
        .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
        .leftJoin(f => f._1 == ar.articleId)
      isFavorite = queryUser
        .join(queryFavoriteArticle)
        .on((u, f) => u.email == lift(viewerEmail) && (f.articleId == ar.articleId) && (f.profileId == u.userId))
        .map(_ => 1)
        .nonEmpty
      pr <- queryProfile if ar.authorId == pr.userId
    } yield (ar, pr, tr.map(_._2), fr.map(_._2), isFavorite))
      .map(_.headOption)
      .map(_.map(mapToArticleData))

  def findBySlugAsSeenBy(articleId: Int, viewerEmail: String): IO[SQLException, Option[ArticleData]] =
    run(for {
      ar <- queryArticle if ar.articleId == lift(articleId)
      tr <- queryTagArticle
        .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
        .leftJoin(a => a._1 == ar.articleId)
      fr <- queryFavoriteArticle
        .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
        .leftJoin(f => f._1 == ar.articleId)
      isFavorite = queryUser
        .join(queryFavoriteArticle)
        .on((u, f) => u.email == lift(viewerEmail) && (f.articleId == ar.articleId) && (f.profileId == u.userId))
        .map(_ => 1)
        .nonEmpty
      pr <- queryProfile if ar.authorId == pr.userId
    } yield (ar, pr, tr.map(_._2), fr.map(_._2), isFavorite))
      .map(_.headOption)
      .map(_.map(mapToArticleData))

  def addTag(tag: String, articleId: Int): IO[Exception, Unit] = run(
    queryTagArticle
      .insert(
        _.tag -> lift(tag),
        _.articleId -> lift(articleId)
      )
  ).unit

  def add(createData: ArticleCreateData, userId: Int): Task[Int] = {
    val now = Instant.now()
    run(
      queryArticle
        .insert(
          _.slug -> lift(createData.title.trim.toLowerCase.replace(" ", "-")),
          _.title -> lift(createData.title.trim),
          _.description -> lift(createData.description),
          _.body -> lift(createData.body),
          _.createdAt -> lift(now),
          _.updatedAt -> lift(now),
          _.authorId -> lift(userId)
        )
        .returning(_.articleId)
    )
      .pipe(mapUniqueConstraintViolationError)
  }

  def updateById(updateData: ArticleData, articleId: Int): Task[Unit] = run(
    queryArticle
      .filter(_.articleId == lift(articleId))
      .update(
        record => record.slug -> lift(updateData.slug),
        record => record.title -> lift(updateData.title),
        record => record.description -> lift(updateData.description),
        record => record.body -> lift(updateData.body),
        record => record.updatedAt -> lift(updateData.updatedAt)
      )
  ).unit
    .pipe(mapUniqueConstraintViolationError)

  def makeFavorite(articleId: Int, userId: Int): Task[Unit] = run(
    queryFavoriteArticle.insertValue(lift(ArticleFavoriteRow(userId, articleId))).onConflictIgnore
  ).unit

  def removeFavorite(articleId: Int, userId: Int): Task[Long] = run(
    queryFavoriteArticle.filter(a => (a.profileId == lift(userId)) && (a.articleId == lift(articleId))).delete
  )

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

  def findComment(commentId: Int): Task[CommentRow] =
    run(queryCommentArticle.filter(_.commentId == lift(commentId)))
      .map(_.headOption)
      .someOrFail(Exceptions.NotFound(s"Comment with ID=$commentId doesn't exist"))

  def deleteComment(commentId: Int): Task[Long] =
    run(queryCommentArticle.filter(_.commentId == lift(commentId)).delete)

  private def article(tuple: (ArticleRow, ProfileRow, Option[String], Option[Int])): ArticleData = {
    val (ar, pr, tags, favorites) = tuple
    ArticleData(
      ar.slug,
      ar.title,
      ar.description,
      ar.body,
      tags.map(explodeTags).getOrElse(List()),
      ar.createdAt,
      ar.updatedAt,
      // TODO implement "favorited" (after authentication is ready)
      favorited = false,
      favorites.getOrElse(0),
      // TODO implement "following" (after authentication is ready)
      ArticleAuthor(pr.username, Option(pr.bio), Option(pr.image), following = false)
    )
  }

  private val mapToArticleData: ((ArticleRow, ProfileRow, Option[String], Option[Int], Boolean)) => ArticleData = {
    case (ar, pr, tags, favorites, isFavorite) =>
      ArticleData(
        ar.slug,
        ar.title,
        ar.description,
        ar.body,
        tags.map(explodeTags).getOrElse(List()),
        ar.createdAt,
        ar.updatedAt,
        favorited = isFavorite,
        favorites.getOrElse(0),
        // TODO implement "following" (after authentication is ready)
        ArticleAuthor(pr.username, Option(pr.bio), Option(pr.image), following = false)
      )
  }

  private def mapUniqueConstraintViolationError[R, A](task: RIO[R, A]): RIO[R, A] = task.mapError {
    case e: SQLiteException if e.getResultCode == SQLITE_CONSTRAINT_UNIQUE =>
      Exceptions.AlreadyInUse("Article name already exists")
    case e => e
  }

object ArticlesRepository:

  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, ArticlesRepository] =
    ZLayer.fromFunction(new ArticlesRepository(_))
