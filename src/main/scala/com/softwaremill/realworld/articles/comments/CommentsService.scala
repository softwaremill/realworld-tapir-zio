package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.comments.CommentsService.*
import com.softwaremill.realworld.articles.core.{ArticleAuthor, ArticlesRepository}
import com.softwaremill.realworld.articles.tags.TagsRepository
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, UserSession}
import com.softwaremill.realworld.users.User
import zio.{Task, ZIO, ZLayer}

class CommentsService(
    commentsRepository: CommentsRepository,
    articlesRepository: ArticlesRepository
):

  def addComment(slug: String, session: UserSession, comment: String): Task[Comment] = for {
    articleId <- articleIdBySlug(slug)
    commentId <- commentsRepository.addComment(articleId, session.userId, comment)
    comment <- commentsRepository.findComment(commentId, session.userId).someOrFail(NotFound(CommentNotFoundMessage(commentId)))
  } yield comment

  def deleteComment(slug: String, session: UserSession, commentId: Int): Task[Unit] = for {
    articleId <- articleIdBySlug(slug)
    tupleWithIds <- commentsRepository
      .findArticleAndAuthorIdsFromComment(commentId)
      .someOrFail(NotFound(ArticleAndAuthorIdsNotFoundMessage(commentId)))
    (commentAuthorId, commentArticleId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized(CommentCannotBeRemoveMessage)).when(session.userId != commentAuthorId)
    _ <- ZIO.fail(BadRequest(CommentNotLinkedToSlugMessage(commentId, slug))).when(articleId != commentArticleId)
    _ <- commentsRepository.deleteComment(commentId)
  } yield ()

  def getCommentsFromArticle(slug: String, sessionOpt: Option[UserSession]): Task[List[Comment]] = {
    articleIdBySlug(slug).flatMap(articleId =>
      sessionOpt match {
        case Some(session) => commentsRepository.findComments(articleId, Some(session.userId))
        case None          => commentsRepository.findComments(articleId, None)
      }
    )
  }

  private def articleIdBySlug(slug: String): Task[Int] =
    articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(ArticleNotFoundMessage(slug)))

object CommentsService:
  private val ArticleNotFoundMessage: String => String = (slug: String) => s"Article with slug $slug doesn't exist."
  private val CommentNotFoundMessage: Int => String = (commentId: Int) => s"Comment with id=$commentId doesn't exist"
  private val CommentCannotBeRemoveMessage = "Can't remove the comment you're not an author of"
  private val CommentNotLinkedToSlugMessage: (Int, String) => String = (commentId: Int, slug: String) =>
    s"Comment with id=$commentId is not linked to slug $slug"
  private val ArticleAndAuthorIdsNotFoundMessage: Int => String = (commentId: Int) =>
    s"ArticleId or authorId for comment with id=$commentId doesn't exist"

  val live: ZLayer[CommentsRepository with ArticlesRepository, Nothing, CommentsService] =
    ZLayer.fromFunction(CommentsService(_, _))
