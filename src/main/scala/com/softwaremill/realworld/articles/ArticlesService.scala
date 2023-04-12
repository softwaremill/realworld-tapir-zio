package com.softwaremill.realworld.articles

import com.softwaremill.realworld.articles.comments.{CommentData, CommentRow}
import com.softwaremill.realworld.articles.model.*
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.profiles.{ProfileData, ProfileRow, ProfilesRepository, ProfilesService}
import com.softwaremill.realworld.users.UserMapper.toUserData
import com.softwaremill.realworld.users.{UserData, UserMapper, UserRow, UserSession, UsersRepository}
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

class ArticlesService(
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository,
    profilesService: ProfilesService
):

  def list(filters: Map[ArticlesFilters, String], pagination: Pagination): IO[SQLException, List[ArticleData]] = articlesRepository
    .list(filters, pagination)

  def findBySlugAsSeenBy(slug: String, email: String): IO[Exception, ArticleData] = articlesRepository
    .findBySlugAsSeenBy(slug, email)
    .flatMap {
      case Some(a) => ZIO.succeed(a)
      case None    => ZIO.fail(Exceptions.NotFound(s"Article with slug $slug doesn't exist."))
    }

  def findBySlugAsSeenBy(articleId: Int, email: String): IO[Exception, ArticleData] = articlesRepository
    .findBySlugAsSeenBy(articleId, email)
    .flatMap {
      case Some(a) => ZIO.succeed(a)
      case None    => ZIO.fail(Exceptions.NotFound(s"Article doesn't exist."))
    }

  def create(createData: ArticleCreateData, userEmail: String): Task[ArticleData] =
    for {
      user <- userByEmail(userEmail)
      articleId <- articlesRepository.add(createData, user.userId)
      _ <- ZIO.foreach(createData.tagList)(tag => articlesRepository.addTag(tag, articleId))
      articleData <- findBySlugAsSeenBy(articleId, userEmail)
    } yield articleData

  def update(articleUpdateData: ArticleUpdateData, slug: String, email: String): Task[ArticleData] =
    for {
      user <- userByEmail(email)
      maybeOldArticle <- articlesRepository.findBySlugAsSeenBy(slug.trim.toLowerCase, email)
      oldArticle <- ZIO.fromOption(maybeOldArticle).mapError(_ => NotFound(s"Article with slug $slug doesn't exist."))
      _ <- ZIO
        .fail(Unauthorized(s"You're not an author of article that you're trying to update"))
        .when(user.username != oldArticle.author.username)
      updatedArticle = updateArticleData(oldArticle, articleUpdateData)
      articleId <- articlesRepository.findArticleIdBySlug(slug)
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
    articleId <- articlesRepository.findArticleIdBySlug(slug)
    _ <- articlesRepository.makeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  def removeFavorite(slug: String, email: String): Task[ArticleData] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug)
    _ <- articlesRepository.removeFavorite(articleId, user.userId)
    articleData <- findBySlugAsSeenBy(slug, email)
  } yield articleData

  private def userByEmail(email: String): Task[UserRow] =
    usersRepository.findByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

  def addComment(slug: String, email: String, comment: String): Task[CommentData] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug)
    id <- articlesRepository.addComment(articleId, user.userId, comment)
    row <- articlesRepository.findComment(id)
    profile <- profilesService.getProfileData(row.authorId, Some(user.userId))
  } yield CommentData(row.commentId, row.createdAt, row.updatedAt, row.body, profile)

  def deleteComment(slug: String, email: String, commentId: Int): Task[Unit] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug)
    comment <- articlesRepository.findComment(commentId)
    _ <- ZIO.fail(BadRequest(s"Comment with ID=$commentId is not linked to slug $slug")).when(comment.articleId != articleId)
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(user.userId != comment.authorId)
    _ <- articlesRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String, userEmailOpt: Option[String]): Task[List[CommentData]] =
    for {
      articleId <- articlesRepository.findArticleIdBySlug(slug)
      commentRowList <- articlesRepository.findComments(articleId)
      commentDataList <- ZIO.collectAllPar(
        commentRowList.map(commentRow =>
          val profile: Task[ProfileData] = userEmailOpt match
            case Some(userEmail) =>
              for {
                user <- userByEmail(userEmail)
                profile <- profilesService.getProfileData(commentRow.authorId, Some(user.userId))
              } yield profile

            case None =>
              for {
                profile <- profilesService.getProfileData(commentRow.authorId, None)
              } yield profile

          profile.map(profile =>
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

object ArticlesService:
  val live: ZLayer[ArticlesRepository with UsersRepository with ProfilesService, Nothing, ArticlesService] =
    ZLayer.fromFunction(ArticlesService(_, _, _))
