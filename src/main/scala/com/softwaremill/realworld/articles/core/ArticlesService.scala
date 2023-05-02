package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{Comment, CommentsRepository}
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

  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."

  def list(filters: ArticlesFilters, pagination: Pagination): IO[SQLException, List[Article]] = articlesRepository
    .list(filters, pagination)

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      email: String
  ): Task[List[Article]] =
    for {
      userId <- userIdByEmail(email)
      foundArticles <- articlesRepository
        .listArticlesByFollowedUsers(pagination, userId)
    } yield foundArticles

  def findBySlugAsSeenBy(slug: String, email: String): Task[Article] = articlesRepository
    .findBySlugAsSeenBy(slug, email)
    .someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  def create(createData: ArticleCreateData, email: String): Task[Article] =
    for {
      userId <- userIdByEmail(email)
      articleId <- articlesRepository.add(createData, userId)
      _ <- ZIO.foreach(createData.tagList) { tagList =>
        ZIO.foreach(tagList)(tag => articlesRepository.addTag(tag, articleId))
      }
      articleData <- findBySlugAsSeenBy(articleId, email)
    } yield articleData

  def delete(slug: String, email: String): Task[Unit] = for {
    userId <- userIdByEmail(email)
    article <- articlesRepository.findArticleBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    _ <- ZIO.fail(Unauthorized("Can't remove the article you're not an author of")).when(userId != article.authorId)
    _ <- commentsRepository.deleteCommentsByArticleId(article.articleId)
    _ <- articlesRepository.deleteFavoritesByArticleId(article.articleId)
    _ <- tagsRepository.deleteTagsByArticleId(article.articleId)
    _ <- articlesRepository.deleteArticle(article.articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): Task[Article] =
    for {
      userId <- userIdByEmail(email)
      oldArticle <- articlesRepository
        .findBySlugAsSeenBy(slug.trim.toLowerCase, email)
        .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
      oldArticleUserId <- userIdByUsername(oldArticle.author.username)
      _ <- ZIO
        .fail(Unauthorized(s"You're not an author of article that you're trying to update"))
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
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  def removeFavorite(slug: String, email: String): Task[Article] = for {
    userId <- userIdByEmail(email)
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, userId)
    articleData <- findBySlugAsSeenBy(slug, email)
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
  private def findBySlugAsSeenBy(articleId: Int, email: String): Task[Article] = articlesRepository
    .findBySlugAsSeenBy(articleId, email)
    .someOrFail(NotFound(ArticleNotFoundMessage(s"$articleId")))
  private def userIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

  private def userIdByUsername(username: String): Task[Int] =
    usersRepository.findUserIdByUsername(username).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

object ArticlesService:
  val live: ZLayer[
    ArticlesRepository with UsersRepository with TagsRepository with CommentsRepository,
    Nothing,
    ArticlesService
  ] =
    ZLayer.fromFunction(ArticlesService(_, _, _, _))
