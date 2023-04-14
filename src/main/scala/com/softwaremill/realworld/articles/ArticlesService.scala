package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.comments.CommentData
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.profiles.{Followers, ProfileRow, ProfilesService}
import com.softwaremill.realworld.tags.TagsRepository
import com.softwaremill.realworld.users.UserMapper.toUserData
import com.softwaremill.realworld.users.{UserData, UserMapper, UserRow, UserSession, UsersRepository}
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository,
    profilesService: ProfilesService,
    tagsRepository: TagsRepository
):

  def list(filters: ArticlesFilters, pagination: Pagination): IO[SQLException, List[ArticleData]] = articlesRepository
    .list(filters, pagination)

  def listArticlesByFollowedUsers(
      pagination: Pagination,
      session: UserSession
  ): Task[List[ArticleData]] =
    for {
      userId <- profilesService.getProfileByEmail(session.email).map(_.userId)
      foundArticles <- articlesRepository
        .listArticlesByFollowedUsers(pagination, userId)
    } yield foundArticles

  def findBySlugAsSeenBy(slug: String, email: String): Task[ArticleData] = articlesRepository
    .findBySlugAsSeenBy(slug, email)
    .flatMap(handleProcessingResult(_, s"Article with slug $slug doesn't exist."))

  def findBySlugAsSeenBy(articleId: Int, email: String): Task[ArticleData] = articlesRepository
    .findBySlugAsSeenBy(articleId, email)
    .flatMap(handleProcessingResult(_, s"Article doesn't exist."))

  def create(createData: ArticleCreateData, userEmail: String): Task[ArticleData] =
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
    articleOpt <- articlesRepository.findArticleBySlug(slug)
    article <- handleProcessingResult(articleOpt, s"Article with slug $slug doesn't exist.")
    _ <- ZIO.fail(Unauthorized("Can't remove the article you're not an author of")).when(user.userId != article.authorId)
    _ <- articlesRepository.deleteCommentsByArticleId(article.articleId)
    _ <- articlesRepository.deleteFavoritesByArticleId(article.articleId)
    _ <- tagsRepository.deleteTagsByArticleId(article.articleId)
    _ <- articlesRepository.deleteArticle(article.articleId)
  } yield ()

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): Task[ArticleData] =
    for {
      user <- userByEmail(email)
      maybeOldArticle <- articlesRepository.findBySlugAsSeenBy(slug.trim.toLowerCase, email)
      oldArticle <- ZIO.fromOption(maybeOldArticle).mapError(_ => NotFound(s"Article with slug $slug doesn't exist."))
      _ <- ZIO
        .fail(Unauthorized(s"You're not an author of article that you're trying to update"))
        .when(user.username != oldArticle.author.username)
      updatedArticle = updateArticleData(oldArticle, articleUpdateData)
      articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
      articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
      _ <- articlesRepository.updateById(updatedArticle, articleId)
    } yield updatedArticle

  private def updateArticleData(articleData: ArticleData, updatedData: ArticleUpdateData): ArticleData = {
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

  def makeFavorite(slug: String, email: String): Task[ArticleData] = for {
    user <- userByEmail(email)
    articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
    articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
    _ <- articlesRepository.makeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  def removeFavorite(slug: String, email: String): Task[ArticleData] = for {
    user <- userByEmail(email)
    articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
    articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
    _ <- articlesRepository.removeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  private def userByEmail(email: String): Task[UserRow] =
    usersRepository.findByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

  def addComment(slug: String, email: String, comment: String): Task[CommentData] = for {
    user <- userByEmail(email)
    articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
    articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
    commentId <- articlesRepository.addComment(articleId, user.userId, comment)
    commentRowOpt <- articlesRepository.findComment(commentId)
    commentRow <- handleProcessingResult(commentRowOpt, s"Comment with ID=$commentId doesn't exist")
    profile <- profilesService.getProfileData(commentRow.authorId, user.userId)
  } yield CommentData(commentRow.commentId, commentRow.createdAt, commentRow.updatedAt, commentRow.body, profile)

  def deleteComment(slug: String, email: String, commentId: Int): Task[Unit] = for {
    user <- userByEmail(email)
    articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
    articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
    commentRowOpt <- articlesRepository.findComment(commentId)
    commentRow <- handleProcessingResult(commentRowOpt, s"Comment with ID=$commentId doesn't exist")
    _ <- ZIO.fail(BadRequest(s"Comment with ID=$commentId is not linked to slug $slug")).when(commentRow.articleId != articleId)
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(user.userId != commentRow.authorId)
    _ <- articlesRepository.deleteComment(commentId)
  } yield ()

  private def handleProcessingResult[T](
      option: Option[T],
      errorMessage: String
  ): Task[T] =
    option match {
      case Some(value) => ZIO.succeed(value)
      case None        => ZIO.fail(Exceptions.NotFound(errorMessage))
    }

object ArticlesService:
  val live: ZLayer[ArticlesRepository with UsersRepository with ProfilesService with TagsRepository, Nothing, ArticlesService] =
    ZLayer.fromFunction(ArticlesService(_, _, _, _))
