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

  def findBySlug(slug: ArticleSlug, userId: Int): Task[Article] =
    articlesRepository
      .findBySlug(slug, userId)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  def create(createData: ArticleCreateData, userId: Int): Task[Article] =
    for {
      _ <- articlesRepository.addArticle(createData, userId)
      article <- findBySlug(ArticleSlug(articlesRepository.convertToSlug(createData.title)), userId)
      _ <- ZIO.logInfo(s"Successfully created article $article")
    } yield article

  def delete(slug: ArticleSlug, userId: Int): Task[Unit] = for {
    tupleWithIds <- articlesRepository
      .findArticleAndAuthorIdsBySlug(slug)
      .someOrFail(NotFound(s"ArticleId or AuthorId for article with slug ${slug.value} doesn't exist"))
    (articleId, authorId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized("Can't remove the article you're not an author of")).when(userId != authorId)
    _ <- articlesRepository.deleteArticle(articleId)
    _ <- ZIO.logInfo(s"Successfully deleted article with id=$articleId")
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: ArticleSlug, userId: Int): Task[Article] = for {
    oldArticle <- articlesRepository
      .findBySlug(slug, userId)
      .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    oldArticleUserId <- userIdByUsername(oldArticle.author.username.value)
    _ <- ZIO
      .fail(Unauthorized("You're not an author of article that you're trying to update"))
      .when(userId != oldArticleUserId)
    updatedArticle = updateArticleData(oldArticle, articleUpdateData)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.updateById(updatedArticle, articleId)
    _ <- ZIO.logInfo(s"Successfully updated article $updatedArticle")
  } yield updatedArticle

  def makeFavorite(slug: ArticleSlug, userId: Int): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.makeFavorite(articleId, userId)
    articleData <- findBySlug(slug, userId)
    _ <- ZIO.logInfo(s"User with id=$userId successfully made favorite article with id=$articleId")
  } yield articleData

  def removeFavorite(slug: ArticleSlug, userId: Int): Task[Article] = for {
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, userId)
    articleData <- findBySlug(slug, userId)
    _ <- ZIO.logInfo(s"User with id=$userId successfully removed from favorite article with id=$articleId")
  } yield articleData

  private def updateArticleData(articleData: Article, updatedData: ArticleUpdateData): Article =
    articleData.copy(
      slug = updatedData.title
        .map(_.toLowerCase)
        .map(_.trim)
        .map(title => title.replace(" ", "-"))
        .map(value => ArticleSlug(value))
        .getOrElse(articleData.slug),
      title = updatedData.title.map(_.trim).getOrElse(articleData.title),
      description = updatedData.description.getOrElse(articleData.description),
      body = updatedData.body.getOrElse(articleData.body),
      updatedAt = Instant.now()
    )

  private def userIdByUsername(username: String): Task[Int] =
    usersRepository.findUserIdByUsername(username).someOrFail(NotFound(s"User with username $username doesn't exist"))

object ArticlesService:
  private def ArticleNotFoundMessage(slug: ArticleSlug): String = s"Article with slug ${slug.value} doesn't exist."

  val live: ZLayer[ArticlesRepository & UsersRepository, Nothing, ArticlesService] =
    ZLayer.fromFunction(ArticlesService(_, _))
