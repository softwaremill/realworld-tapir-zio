package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.{ArticleAuthor, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.users.{UserRow, UsersRepository}
import zio.{Task, ZIO, ZLayer}

class CommentsService(
    commentsRepository: CommentsRepository,
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository
):

  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val CommentNotFoundMessage = (commentId: Int) => s"Comment with ID=$commentId doesn't exist"

  def addComment(slug: String, email: String, comment: String): Task[Comment] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    commentId <- commentsRepository.addComment(articleId, user.userId, comment)
    commentRow <- commentsRepository.findComment(commentId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
    commentAuthor <- getCommentAuthor(commentRow.authorId, Some(user.userId))
  } yield Comment(commentRow.commentId, commentRow.createdAt, commentRow.updatedAt, commentRow.body, commentAuthor)

  def deleteComment(slug: String, email: String, commentId: Int): Task[Unit] = for {
    user <- userByEmail(email)
    articleId <- articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))
    commentRow <- commentsRepository.findComment(commentId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
    _ <- ZIO.fail(BadRequest(s"Comment with ID=$commentId is not linked to slug $slug")).when(commentRow.articleId != articleId)
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(user.userId != commentRow.authorId)
    _ <- commentsRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String, userEmailOpt: Option[String]): Task[List[Comment]] =
    for {
      articleIdOpt <- articlesRepository.findArticleIdBySlug(slug)
      articleId <- handleProcessingResult(articleIdOpt, s"Article with slug $slug doesn't exist.")
      commentRowList <- commentsRepository.findComments(articleId)
      commentDataList <- ZIO.collectAllPar(
        commentRowList.map(commentRow =>
          (userEmailOpt match
            case Some(userEmail) =>
              for {
                user <- userByEmail(userEmail)
                commentAuthor <- getCommentAuthor(commentRow.authorId, Some(user.userId))
              } yield commentAuthor

            case None => getCommentAuthor(commentRow.authorId, None)
          ).map(commentAuthor =>
            Comment(
              id = commentRow.commentId,
              createdAt = commentRow.createdAt,
              updatedAt = commentRow.updatedAt,
              body = commentRow.body,
              author = commentAuthor
            )
          )
        )
      )
    } yield commentDataList

  private def getCommentAuthor(profileId: Int, asSeenByUserWithIdOpt: Option[Int]): Task[CommentAuthor] =
    usersRepository
      .findById(profileId)
      .someOrFail(NotFound(s"Couldn't find a profile for user with id=$profileId"))
      .flatMap(userRow =>
        asSeenByUserWithIdOpt match
          case Some(asSeenByUserWithId) =>
            usersRepository
              .isFollowing(userRow.userId, asSeenByUserWithId)
              .map(CommentAuthor(userRow.username, userRow.bio, userRow.image, _))
          case None => ZIO.succeed(CommentAuthor(userRow.username, userRow.bio, userRow.image, false))
      )

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

object CommentsService:
  val live: ZLayer[CommentsRepository with ArticlesRepository with UsersRepository, Nothing, CommentsService] =
    ZLayer.fromFunction(CommentsService(_, _, _))
