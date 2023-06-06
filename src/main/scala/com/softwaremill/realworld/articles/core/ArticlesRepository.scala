package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.*
import com.softwaremill.realworld.articles.core.api.ArticleCreateData
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import io.getquill.*
import io.getquill.jdbczio.*
import org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
import org.sqlite.{SQLiteErrorCode, SQLiteException}
import zio.{IO, RIO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import scala.collection.immutable
import scala.util.chaining.*

case class ArticleFavoriteRow(profileId: Int, articleId: Int)
case class ArticleTagRow(tag: String, articleId: Int)
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
case class ProfileRow(userId: Int, username: String, bio: String, image: String)
case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: Option[String],
    image: Option[String]
)
case class ArticleQueryBuildSupport(
    articleRow: ArticleRow,
    profileRow: ProfileRow,
    tagsOpt: Option[String],
    favoritedCountOpt: Option[Int],
    isFavorite: Boolean,
    isFollowing: Boolean
)

class ArticlesRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryArticle = quote(querySchema[ArticleRow](entity = "articles"))
  private inline def queryComment = quote(querySchema[CommentRow](entity = "comments_articles"))
  private inline def queryFavoriteArticle = quote(querySchema[ArticleFavoriteRow](entity = "favorites_articles"))
  private inline def queryFollower = quote(querySchema[FollowerRow](entity = "followers"))
  private inline def queryProfile = quote(querySchema[ProfileRow](entity = "users"))
  private inline def queryTagArticle = quote(querySchema[ArticleTagRow](entity = "tags_articles"))
  private inline def queryUser = quote(querySchema[UserRow](entity = "users"))
  private inline def tagsConcat: Quoted[String => String] = quote { (str: String) =>
    sql"GROUP_CONCAT(($str), '|')".pure.as[String]
  }

  def list(filters: ArticlesFilters, pagination: Pagination, viewerIdOpt: Option[Int]): IO[SQLException, List[Article]] = {
    val tagFilter = filters.tag.getOrElse("")
    val authorFilter = filters.author.getOrElse("")
    val favoritedFilter = filters.favorited.getOrElse("")

    val articleRow: Quoted[Query[ArticleRow]] = quote {
      sql"""
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
    }

    val articleQuery = viewerIdOpt match
      case Some(viewerId) =>
        buildArticleQueryWithFavoriteAndFollowing(articleRow, viewerId)
      case None =>
        buildArticleQuery(articleRow)

    run(articleQuery).map(_.map(article))
  }

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      viewerId: Int
  ): IO[SQLException, List[Article]] = {
    val articleRow: Quoted[Query[ArticleRow]] = quote {
      sql"""
               SELECT DISTINCT * FROM articles a
               WHERE a.author_id IN (SELECT f.user_id FROM followers f
                                     WHERE f.follower_id = ${lift(viewerId)})
             """
        .as[Query[ArticleRow]]
        .drop(lift(pagination.offset))
        .take(lift(pagination.limit))
        .sortBy(ar => ar.slug)
    }

    val articleQuery = buildArticleQueryWithFavoriteAndFollowing(articleRow, viewerId)

    run(articleQuery).map(_.map(article))
  }

  def findBySlug(slug: String, viewerId: Int): IO[SQLException, Option[Article]] = {
    val articleRow: Quoted[EntityQuery[ArticleRow]] = quote { queryArticle.filter(ar => ar.slug == lift(slug)) }
    val articleQuery = buildArticleQueryWithFavoriteAndFollowing(articleRow, viewerId)

    run(articleQuery)
      .map(_.headOption)
      .map(_.map(article))
  }

  def findArticleIdBySlug(slug: String): IO[SQLException, Option[Int]] =
    run(
      queryArticle
        .filter(a => a.slug == lift(slug))
        .map(_.articleId)
    )
      .map(_.headOption)

  def findArticleAndAuthorIdsBySlug(slug: String): IO[SQLException, Option[(Int, Int)]] =
    run(queryArticle.filter(a => a.slug == lift(slug)))
      .map(
        _.headOption
          .map(ar => (ar.articleId, ar.authorId))
      )

  def addArticle(createData: ArticleCreateData, userId: Int): Task[Unit] = {
    val now = Instant.now()
    val tags = createData.tagList.getOrElse(Nil)

    val addArticle: Quoted[ActionReturning[ArticleRow, Index]] = quote {
      queryArticle
        .insert(
          _.slug -> lift(convertToSlug(createData.title)),
          _.title -> lift(createData.title.trim),
          _.description -> lift(createData.description),
          _.body -> lift(createData.body),
          _.createdAt -> lift(now),
          _.updatedAt -> lift(now),
          _.authorId -> lift(userId)
        )
        .returning[Int](_.articleId)
    }

    def addTags(tags: List[String], articleId: Int): Quoted[BatchAction[Insert[ArticleTagRow]]] =
      quote {
        liftQuery(tags).foreach(tag => queryTagArticle.insertValue(ArticleTagRow(tag, lift(articleId))))
      }

    transaction {
      run(addArticle)
        .pipe(mapUniqueConstraintViolationError)
        .flatMap(articleId => run(addTags(tags, articleId)))
        .unit
    }
  }

  def deleteArticle(articleId: Int): Task[Long] = {
    val deleteComments: DynamicDelete[CommentRow] = queryComment.dynamic.filter(_.articleId == lift(articleId)).delete
    val deleteFavorites: DynamicDelete[ArticleFavoriteRow] = queryFavoriteArticle.dynamic.filter(_.articleId == lift(articleId)).delete
    val deleteTags: DynamicDelete[ArticleTagRow] = queryTagArticle.dynamic.filter(_.articleId == lift(articleId)).delete
    val deleteArticle: DynamicDelete[ArticleRow] = queryArticle.dynamic.filter(_.articleId == lift(articleId)).delete

    transaction {
      run(deleteComments)
        .flatMap(_ => run(deleteFavorites))
        .flatMap(_ => run(deleteTags))
        .flatMap(_ => run(deleteArticle))
    }
  }

  def updateById(updateData: Article, articleId: Int): Task[Unit] = run(
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

  val convertToSlug: String => String = (title: String) => title.trim.toLowerCase.replace(" ", "-")

  private def explodeTags(tags: String): List[String] = tags.split("\\|").toList

  private def article(as: ArticleQueryBuildSupport): Article =
    Article(
      slug = as.articleRow.slug,
      title = as.articleRow.title,
      description = as.articleRow.description,
      body = as.articleRow.body,
      tagList = as.tagsOpt.map(explodeTags).map(_.sorted).getOrElse(List()),
      createdAt = as.articleRow.createdAt,
      updatedAt = as.articleRow.updatedAt,
      favorited = as.isFavorite,
      favoritesCount = as.favoritedCountOpt.getOrElse(0),
      author = ArticleAuthor(as.profileRow.username, Option(as.profileRow.bio), Option(as.profileRow.image), following = as.isFollowing)
    )

  private def buildArticleQuery(arq: Quoted[Query[ArticleRow]]) =
    quote {
      for {
        ar <- arq
        pr <- queryProfile.join(pr => ar.authorId == pr.userId)
        tr <- queryTagArticle
          .groupByMap(_.articleId)(atr => (atr.articleId, tagsConcat(atr.tag)))
          .leftJoin(a => a._1 == ar.articleId)
        fr <- queryFavoriteArticle
          .groupByMap(_.articleId)(fr => (fr.articleId, count(fr.profileId)))
          .leftJoin(f => f._1 == ar.articleId)
      } yield ArticleQueryBuildSupport(
        articleRow = ar,
        profileRow = pr,
        tagsOpt = tr.map(_._2),
        favoritedCountOpt = fr.map(_._2),
        isFavorite = false,
        isFollowing = false
      )
    }

  private def buildArticleQueryWithFavoriteAndFollowing(arq: Quoted[Query[ArticleRow]], viewerId: Int) =
    quote {
      for {
        as <- buildArticleQuery(arq)
        isFavorite = queryUser
          .join(queryFavoriteArticle)
          .on((u, f) => u.userId == lift(viewerId) && (f.articleId == as.articleRow.articleId) && (f.profileId == u.userId))
          .map(_ => 1)
          .nonEmpty
        isFollowing = queryFollower
          .filter(f => (f.userId == as.profileRow.userId) && (f.followerId == lift(viewerId)))
          .map(_ => 1)
          .nonEmpty
      } yield ArticleQueryBuildSupport(
        articleRow = as.articleRow,
        profileRow = as.profileRow,
        tagsOpt = as.tagsOpt,
        favoritedCountOpt = as.favoritedCountOpt,
        isFavorite = isFavorite,
        isFollowing = isFollowing
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
