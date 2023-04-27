package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{Comment, CommentsRepository}
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData}
import com.softwaremill.realworld.articles.core.{Article, ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.users.{UserRow, UsersRepository, UsersService}
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository,
    usersService: UsersService,
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
      userId <- usersService.getProfileByEmail(email).map(_.userId)
      foundArticles <- articlesRepository
        .listArticlesByFollowedUsers(pagination, userId)
    } yield foundArticles

  def findBySlugAsSeenBy(slug: String, email: String): Task[Article] = articlesRepository
    .findBySlugAsSeenBy(slug, email)
    .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))

  def findBySlugAsSeenBy(articleId: Int, email: String): Task[Article] = articlesRepository
    .findBySlugAsSeenBy(articleId, email)
    .someOrFail(Exceptions.NotFound(s"Article doesn't exist."))

  def create(createData: ArticleCreateData, userEmail: String): Task[Article] =
    for {
      user <- userByEmail(userEmail)
      articleId <- articlesRepository.add(createData, user.userId)
      _ <- ZIO.foreach(createData.tagList) { tagList =>
        ZIO.foreach(tagList)(tag => articlesRepository.addTag(tag, articleId))
      }
      articleData <- findBySlugAsSeenBy(articleId, userEmail)
    } yield articleData

  def delete(slug: String, email: String): Task[Unit] = for {
    user <- userByEmail(email)
    article <- articlesRepository.findArticleBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    _ <- ZIO.fail(Unauthorized("Can't remove the article you're not an author of")).when(user.userId != article.authorId)
    _ <- commentsRepository.deleteCommentsByArticleId(article.articleId)
    _ <- articlesRepository.deleteFavoritesByArticleId(article.articleId)
    _ <- tagsRepository.deleteTagsByArticleId(article.articleId)
    _ <- articlesRepository.deleteArticle(article.articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): Task[Article] =
    for {
      user <- userByEmail(email)
      oldArticle <- articlesRepository
        .findBySlugAsSeenBy(slug.trim.toLowerCase, email)
        .someOrFail(NotFound(ArticleNotFoundMessage(slug)))
      _ <- ZIO
        .fail(Unauthorized(s"You're not an author of article that you're trying to update"))
        .when(user.username != oldArticle.author.username)
      updatedArticle = updateArticleData(oldArticle, articleUpdateData)
      articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
      _ <- articlesRepository.updateById(updatedArticle, articleId)
    } yield updatedArticle

  def makeFavorite(slug: String, email: String): Task[Article] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.makeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  def removeFavorite(slug: String, email: String): Task[Article] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository
      .findArticleIdBySlug(slug)
      .someOrFail(Exceptions.NotFound(ArticleNotFoundMessage(slug)))
    _ <- articlesRepository.removeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  private def userByEmail(email: String): Task[UserRow] =
    usersRepository.findByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

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

object ArticlesService:
  val live: ZLayer[
    ArticlesRepository with UsersRepository with UsersService with TagsRepository with CommentsRepository,
    Nothing,
    ArticlesService
  ] =
    ZLayer.fromFunction(ArticlesService(_, _, _, _, _))
