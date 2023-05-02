package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.{ArticleAuthor, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.users.{User, UsersRepository}
import zio.{Task, ZIO, ZLayer}

class CommentsService(
    commentsRepository: CommentsRepository,
    articlesRepository: ArticlesRepository,
    usersRepository: UsersRepository
):

  def addComment(slug: String, email: String, comment: String): Task[Comment] = for {
    userId <- userIdByEmail(email)
    articleId <- articleIdBySlug(slug)
    commentId <- commentsRepository.addComment(articleId, userId, comment)
    comment <- commentsRepository.findComment(commentId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
  } yield comment

  def deleteComment(slug: String, email: String, commentId: Int): Task[Unit] = for {
    userId <- userIdByEmail(email)
    articleId <- articleIdBySlug(slug)
    tupleWithIds <- commentsRepository
      .findArticleAndAuthorIdsFromComment(commentId)
      .someOrFail(NotFound(ArticleAndAuthorIdsNotFoundMessage(commentId)))
    (commentAuthorId, commentArticleId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized(CommentCannotBeRemoveMessage)).when(userId != commentAuthorId)
    _ <- ZIO.fail(BadRequest(CommentNotLinkedToSlugMessage(commentId, slug))).when(articleId != commentArticleId)
    _ <- commentsRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String): Task[List[Comment]] =
    for {
      articleId <- articleIdBySlug(slug)
      commentList <- commentsRepository.findComments(articleId)
    } yield commentList

  private def userIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(NotFound(UserNotFoundMessage(email)))

  private def articleIdBySlug(slug: String): Task[Int] =
    articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))

  // Todo some comments are the same in repositories, should i put it in utils?
  // Todo is it a good idea to create method for each message?
  private val ArticleNotFoundMessage: String => String = (slug: String) => s"Article with slug $slug doesn't exist."
  private val CommentNotFoundMessage: Int => String = (commentId: Int) => s"Comment with id=$commentId doesn't exist"
  private val UserNotFoundMessage: String => String = (email: String) => s"User with email $email doesn't exist"
  private val CommentCannotBeRemoveMessage = "Can't remove the comment you're not an author of"
  private val CommentNotLinkedToSlugMessage: (Int, String) => String = (commentId: Int, slug: String) =>
    s"Comment with id=$commentId is not linked to slug $slug"
  private val ArticleAndAuthorIdsNotFoundMessage: Int => String = (commentId: Int) =>
    s"ArticleId or authorId for comment with id=$commentId doesn't exist"

object CommentsService:
  val live: ZLayer[CommentsRepository with ArticlesRepository with UsersRepository, Nothing, CommentsService] =
    ZLayer.fromFunction(CommentsService(_, _, _))
