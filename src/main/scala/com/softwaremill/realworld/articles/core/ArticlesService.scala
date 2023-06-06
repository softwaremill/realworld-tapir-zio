package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.core.ArticlesService.*
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData}
import com.softwaremill.realworld.common.Exceptions.{NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.users.UsersRepository
import zio.{Task, ZIO, ZLayer}

import java.time.Instant

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository
):

  def list(filters: ArticlesFilters, pagination: Pagination, userIdOpt: Option[Int]): Task[List[Article]] =
    userIdOpt match
      case Some(userId) => articlesRepository.list(filters, pagination, Some(userId))
      case None         => articlesRepository.list(filters, pagination, None)

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      userId: Int
  ): Task[List[Article]] =
    articlesRepository
      .listArticlesByFollowedUsers(pagination, userId)

  def findBySlug(slug: String, userId: Int): Task[Article] =
    articlesRepository
      .findBySlug(slug, userId)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  def create(createData: ArticleCreateData, userId: Int): Task[Article] =
    for {
      _ <- articlesRepository.addArticle(createData, userId)
      articleData <- findBySlug(articlesRepository.convertToSlug(createData.title), userId)
    } yield articleData

  def delete(slug: String, userId: Int): Task[Unit] = for {
    tupleWithIds <- articlesRepository
      .findArticleAndAuthorIdsBySlug(slug)
      .someOrFail(NotFound(ArticleAndAuthorIdsNotFoundMessage(slug)))
    (articleId, authorId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized(ArticleCannotBeRemovedMessage)).when(userId != authorId)
    _ <- articlesRepository.deleteArticle(articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, userId: Int): Task[Article] = for {
    oldArticle <- articlesRepository
      .findBySlug(slug.trim.toLowerCase, userId)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    oldArticleUserId <- userIdByUsername(oldArticle.author.username)
    _ <- ZIO
      .fail(Unauthorized(ArticleCannotBeUpdatedMessage))
      .when(userId != oldArticleUserId)
    updatedArticle = updateArticleData(oldArticle, articleUpdateData)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.updateById(updatedArticle, articleId)
  } yield updatedArticle

  def makeFavorite(slug: String, userId: Int): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.makeFavorite(articleId, userId)
    articleData <- findBySlug(slug, userId)
  } yield articleData

  def removeFavorite(slug: String, userId: Int): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, userId)
    articleData <- findBySlug(slug, userId)
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

  private def userIdByUsername(username: String): Task[Int] =
    usersRepository.findUserIdByUsername(username).someOrFail(NotFound(UserWithUsernameNotFoundMessage(username)))

object ArticlesService:
  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val UserWithUsernameNotFoundMessage: String => String = (username: String) => s"User with username $username doesn't exist"
  private val ArticleAndAuthorIdsNotFoundMessage = (slug: String) => s"ArticleId or AuthorId for article with slug $slug doesn't exist"
  private val ArticleCannotBeRemovedMessage: String = "Can't remove the article you're not an author of"
  private val ArticleCannotBeUpdatedMessage: String = "You're not an author of article that you're trying to update"

  val live: ZLayer[ArticlesRepository with UsersRepository, Nothing, ArticlesService] =
    ZLayer.fromFunction(ArticlesService(_, _))
