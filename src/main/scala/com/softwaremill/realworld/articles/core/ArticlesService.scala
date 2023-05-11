package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{Comment, CommentsRepository}
import com.softwaremill.realworld.articles.core.ArticlesService.*
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData}
import com.softwaremill.realworld.articles.core.{Article, ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
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

  def list(filters: ArticlesFilters, pagination: Pagination, emailOpt: Option[String]): Task[List[Article]] =
    emailOpt match
      case Some(email) =>
        userIdByEmail(email)
          .flatMap(userId =>
            articlesRepository
              .list(filters, pagination, Some(userId, email))
          )
      case None => articlesRepository.list(filters, pagination, None)

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      email: String
  ): Task[List[Article]] =
    for {
      userId <- userIdByEmail(email)
      foundArticles <- articlesRepository
        .listArticlesByFollowedUsers(pagination, (userId, email))
    } yield foundArticles

  def findBySlug(slug: String, email: String): Task[Article] =
    for {
      userId <- userIdByEmail(email)
      foundArticle <- articlesRepository
        .findBySlug(slug, (userId, email))
        .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    } yield foundArticle

  def create(createData: ArticleCreateData, email: String): Task[Article] =
    for {
      userId <- userIdByEmail(email)
      articleId <- articlesRepository.add(createData, userId)
      _ <- ZIO.foreach(createData.tagList) { tagList =>
        ZIO.foreach(tagList)(tag => articlesRepository.addTag(tag, articleId))
      }
      articleData <- findBySlug(articlesRepository.convertToSlug(createData.title), (userId, email))
    } yield articleData

  def delete(slug: String, email: String): Task[Unit] = for {
    userId <- userIdByEmail(email)
    tupleWithIds <- articlesRepository
      .findArticleAndAuthorIdsBySlug(slug)
      .someOrFail(NotFound(ArticleAndAuthorIdsNotFoundMessage(slug)))
    (articleId, authorId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized(ArticleCannotBeRemovedMessage)).when(userId != authorId)
    _ <- commentsRepository.deleteCommentsByArticleId(articleId)
    _ <- articlesRepository.deleteFavoritesByArticleId(articleId)
    _ <- tagsRepository.deleteTagsByArticleId(articleId)
    _ <- articlesRepository.deleteArticle(articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): Task[Article] =
    for {
      userId <- userIdByEmail(email)
      oldArticle <- articlesRepository
        .findBySlug(slug.trim.toLowerCase, (userId, email))
        .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
      oldArticleUserId <- userIdByUsername(oldArticle.author.username)
      _ <- ZIO
        .fail(Unauthorized(ArticleCannotBeUpdatedMessage))
        .when(userId != oldArticleUserId)
      updatedArticle = updateArticleData(oldArticle, articleUpdateData)
      articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
      _ <- articlesRepository.updateById(updatedArticle, articleId)
    } yield updatedArticle

  def makeFavorite(slug: String, email: String): Task[Article] = for {
    userId <- userIdByEmail(email)
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.makeFavorite(articleId, userId)
    articleData <- findBySlug(slug, (userId, email))
  } yield articleData

  def removeFavorite(slug: String, email: String): Task[Article] = for {
    userId <- userIdByEmail(email)
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, userId)
    articleData <- findBySlug(slug, (userId, email))
  } yield articleData

  private def updateArticleData(articleData: Article, updatedData: ArticleUpdateData): Article = {
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
  }

  private def findBySlug(slug: String, viewerData: (Int, String)): Task[Article] =
    articlesRepository
      .findBySlug(slug, viewerData)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  private def userIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(NotFound(UserWithEmailNotFoundMessage(email)))

  private def userIdByUsername(username: String): Task[Int] =
    usersRepository.findUserIdByUsername(username).someOrFail(NotFound(UserWithUsernameNotFoundMessage(username)))

object ArticlesService:
  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val UserWithEmailNotFoundMessage: String => String = (email: String) => s"User with email $email doesn't exist"
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
