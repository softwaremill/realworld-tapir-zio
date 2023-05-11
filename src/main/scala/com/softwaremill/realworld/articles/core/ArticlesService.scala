package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{Comment, CommentsRepository}
import com.softwaremill.realworld.articles.core.ArticlesService.*
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData}
import com.softwaremill.realworld.articles.core.{Article, ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination, UserSession}
import com.softwaremill.realworld.users.{User, UsersRepository, UsersService}
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository,
    tagsRepository: TagsRepository,
    commentsRepository: CommentsRepository
):

  def list(filters: ArticlesFilters, pagination: Pagination, sessionOpt: Option[UserSession]): Task[List[Article]] =
    sessionOpt match
      case Some(session) => articlesRepository.list(filters, pagination, Some((session.userId, session.userEmail)))
      case None          => articlesRepository.list(filters, pagination, None)

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      session: UserSession
  ): Task[List[Article]] =
    articlesRepository
      .listArticlesByFollowedUsers(pagination, (session.userId, session.userEmail))

  def findBySlug(slug: String, session: UserSession): Task[Article] =
    articlesRepository
      .findBySlug(slug, (session.userId, session.userEmail))
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  def create(createData: ArticleCreateData, session: UserSession): Task[Article] = for {
    articleId <- articlesRepository.add(createData, session.userId)
    _ <- ZIO.foreach(createData.tagList) { tagList =>
      ZIO.foreach(tagList)(tag => articlesRepository.addTag(tag, articleId))
    }
    articleData <- findBySlug(articlesRepository.convertToSlug(createData.title), (session.userId, session.userEmail))
  } yield articleData

  def delete(slug: String, session: UserSession): Task[Unit] = for {
    tupleWithIds <- articlesRepository
      .findArticleAndAuthorIdsBySlug(slug)
      .someOrFail(NotFound(ArticleAndAuthorIdsNotFoundMessage(slug)))
    (articleId, authorId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized(ArticleCannotBeRemovedMessage)).when(session.userId != authorId)
    _ <- commentsRepository.deleteCommentsByArticleId(articleId)
    _ <- articlesRepository.deleteFavoritesByArticleId(articleId)
    _ <- tagsRepository.deleteTagsByArticleId(articleId)
    _ <- articlesRepository.deleteArticle(articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, session: UserSession): Task[Article] = for {
    oldArticle <- articlesRepository
      .findBySlug(slug.trim.toLowerCase, (session.userId, session.userEmail))
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    oldArticleUserId <- userIdByUsername(oldArticle.author.username)
    _ <- ZIO
      .fail(Unauthorized(ArticleCannotBeUpdatedMessage))
      .when(session.userId != oldArticleUserId)
    updatedArticle = updateArticleData(oldArticle, articleUpdateData)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.updateById(updatedArticle, articleId)
  } yield updatedArticle

  def makeFavorite(slug: String, session: UserSession): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.makeFavorite(articleId, session.userId)
    articleData <- findBySlug(slug, (session.userId, session.userEmail))
  } yield articleData

  def removeFavorite(slug: String, session: UserSession): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, session.userId)
    articleData <- findBySlug(slug, (session.userId, session.userEmail))
  } yield articleData

  private def updateArticleData(articleData: Article, updatedData: ArticleUpdateData): Article =
    articleData.copy(
      slug = updatedData.title
        .map(_.toLowerCase)
        .map(_.trim)
        .map(title => title.replace(" ", "-"))
        .getOrElse(articleData.slug),
      title = updatedData.title.map(_.trim).getOrElse(articleData.title),
      description = updatedData.description.getOrElse(articleData.description),
      body = updatedData.body.getOrElse(articleData.body),
      updatedAt = Instant.now()
    )

  private def findBySlug(slug: String, viewerData: (Int, String)): Task[Article] =
    articlesRepository
      .findBySlug(slug, viewerData)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  private def userIdByUsername(username: String): Task[Int] =
    usersRepository.findUserIdByUsername(username).someOrFail(NotFound(UserWithUsernameNotFoundMessage(username)))

object ArticlesService:
  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val UserWithUsernameNotFoundMessage: String => String = (username: String) => s"User with username $username doesn't exist"
  private val ArticleAndAuthorIdsNotFoundMessage = (slug: String) => s"ArticleId or AuthorId for article with slug $slug doesn't exist"
  private val ArticleCannotBeRemovedMessage: String = "Can't remove the article you're not an author of"
  private val ArticleCannotBeUpdatedMessage: String = "You're not an author of article that you're trying to update"

  val live: ZLayer[
    ArticlesRepository with UsersRepository with TagsRepository with CommentsRepository,
    Nothing,
    ArticlesService
  ] =
    ZLayer.fromFunction(ArticlesService(_, _, _, _))
