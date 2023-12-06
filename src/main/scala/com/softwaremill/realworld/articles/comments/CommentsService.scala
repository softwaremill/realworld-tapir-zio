package com.softwaremill.realworld.articles.comments

import com.softwaremill.realworld.articles.core.{ArticleSlug, ArticlesRepository}
import com.softwaremill.realworld.common.Exceptions
import com.softwaremill.realworld.common.Exceptions.{BadRequest, NotFound, Unauthorized}
import zio.{Task, ZIO, ZLayer}

class CommentsService(
    commentsRepository: CommentsRepository,
    articlesRepository: ArticlesRepository
):

  def addComment(slug: ArticleSlug, userId: Int, comment: String): Task[Comment] = for {
    articleId <- articleIdBySlug(slug)
    commentId <- commentsRepository.addComment(articleId, userId, comment)
    comment <- commentsRepository.findComment(commentId, userId).someOrFail(NotFound(s"Comment with id=$commentId doesn't exist"))
    _ <- ZIO.logInfo(s"Successfully created comment $comment")
  } yield comment

  def deleteComment(slug: ArticleSlug, userId: Int, commentId: CommentId): Task[Unit] = for {
    articleId <- articleIdBySlug(slug)
    tupleWithIds <- commentsRepository
      .findArticleAndAuthorIdsFromComment(commentId)
      .someOrFail(NotFound(s"ArticleId or authorId for comment with id=${commentId.value} doesn't exist"))
    (commentAuthorId, commentArticleId) = tupleWithIds
    _ <- ZIO.fail(Unauthorized("Can't remove the comment you're not an author of")).when(userId != commentAuthorId)
    _ <- ZIO.fail(BadRequest(s"Comment with id=${commentId.value} is not linked to slug ${slug.value}")).when(articleId != commentArticleId)
    _ <- commentsRepository.deleteComment(commentId)
    _ <- ZIO.logInfo(s"Successfully deleted comment with id=${commentId.value}")
  } yield ()

  def getCommentsFromArticle(slug: ArticleSlug, userIdOpt: Option[Int]): Task[List[Comment]] =
    articleIdBySlug(slug).flatMap(articleId =>
      userIdOpt match {
        case Some(userId) => commentsRepository.findComments(articleId, Some(userId))
        case None         => commentsRepository.findComments(articleId, None)
      }
    )

  private def articleIdBySlug(slug: ArticleSlug): Task[Int] =
    articlesRepository.findArticleIdBySlug(slug).someOrFail(NotFound(s"Article with slug ${slug.value} doesn't exist."))

object CommentsService:

  val live: ZLayer[CommentsRepository & ArticlesRepository, Nothing, CommentsService] =
    ZLayer.fromFunction(CommentsService(_, _))
