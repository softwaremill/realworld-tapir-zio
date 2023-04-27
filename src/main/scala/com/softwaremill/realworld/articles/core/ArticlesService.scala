package com.softwaremill.realworld.articles.core

import com.softwaremill.realworld.articles.comments.{CommentData, CommentRow}
import com.softwaremill.realworld.articles.core.api.{ArticleCreateData, ArticleUpdateData}
import com.softwaremill.realworld.articles.core.{Article, ArticlesFilters, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.db.UserRow
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.users.{UsersRepository, UsersService}
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository,
    usersService: UsersService,
    tagsRepository: TagsRepository
):

  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val CommentNotFoundMessage = (commentId: Int) => s"Comment with ID=$commentId doesn't exist"

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
    _ <- articlesRepository.deleteCommentsByArticleId(article.articleId)
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

  def addComment(slug: String, email: String, comment: String): Task[CommentData] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    commentId <- articlesRepository.addComment(articleId, user.userId, comment)
    commentRow <- articlesRepository.findComment(commentId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
    profile <- usersService.getProfileData(commentRow.authorId, Some(user.userId))
  } yield CommentData(commentRow.commentId, commentRow.createdAt, commentRow.updatedAt, commentRow.body, profile)

  def deleteComment(slug: String, email: String, commentId: Int): Task[Unit] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    commentRow <- articlesRepository.findComment(commentId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
    _ <- ZIO.fail(BadRequest(s"Comment with ID=$commentId is not linked to slug $slug")).when(commentRow.articleId != articleId)
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(user.userId != commentRow.authorId)
    _ <- articlesRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String, userEmailOpt: Option[String]): Task[List[CommentData]] =
    for {
      articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
      articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
      commentRowList <- articlesRepository.findComments(articleId)
      commentDataList <- ZIO.collectAllPar(
        commentRowList.map(commentRow =>
          (userEmailOpt match
            case Some(userEmail) =>
              for {
                user <- userByEmail(userEmail)
                profile <- usersService.getProfileData(commentRow.authorId, Some(user.userId))
              } yield profile

            case None => usersService.getProfileData(commentRow.authorId, None)
          ).map(profile =>
            CommentData(
              id = commentRow.commentId,
              createdAt = commentRow.createdAt,
              updatedAt = commentRow.updatedAt,
              body = commentRow.body,
              author = profile
            )
          )
        )
      )
    } yield commentDataList

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

  private def userByEmail(email: String): Task[UserRow] =
    usersRepository.findByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

  private def handleProcessingResult[T](
      option: Option[T],
      errorMessage: String
  ): Task[T] =
    option match {
      case Some(value) => ZIO.succeed(value)
      case None        => ZIO.fail(Exceptions.NotFound(errorMessage))
    }

object ArticlesService:
  val live: ZLayer[ArticlesRepository with UsersRepository with UsersService with TagsRepository, Nothing, ArticlesService] =
    ZLayer.fromFunction(ArticlesService(_, _, _, _))
