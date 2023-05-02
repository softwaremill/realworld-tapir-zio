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

  private val ArticleNotFoundMessage = (slug: String) => s"Article with slug $slug doesn't exist."
  private val CommentNotFoundMessage = (commentId: Int) => s"Comment with ID=$commentId doesn't exist"
  private val ArticleAndAuthorIdsNotFoundMessage = (commentId: Int) => s"ArticleId or AuthorId for comment with ID=$commentId doesn't exist"

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
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(userId != commentAuthorId)
    _ <- ZIO.fail(BadRequest(s"Comment with ID=$commentId is not linked to slug $slug")).when(articleId != commentArticleId)
    _ <- commentsRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String): Task[List[Comment]] =
    for {
      articleId <- articleIdBySlug(slug)
      commentList <- commentsRepository.findComments(articleId)
    } yield commentList

  private def userIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(NotFound("User doesn't exist, re-login may be needed!"))

  private def articleIdBySlug(slug: String): Task[Int] =
    articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))

object CommentsService:
  val live: ZLayer[CommentsRepository with ArticlesRepository with UsersRepository, Nothing, CommentsService] =
    ZLayer.fromFunction(CommentsService(_, _, _))
