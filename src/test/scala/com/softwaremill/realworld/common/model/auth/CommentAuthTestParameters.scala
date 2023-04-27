package com.softwaremill.realworld.common.model.auth

import com.softwaremill.realworld.articles.comments.CommentsServerEndpoints
import com.softwaremill.realworld.articles.comments.api.CommentsEndpoints
import com.softwaremill.realworld.articles.core.ArticlesEndpoints
import sttp.client3.{Request, UriContext, basicRequest}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO

class CommentAuthTestParameters(
    val endpoint: ZIO[CommentsServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    val request: Request[Either[String, String], Any],
    val headers: Map[String, String],
    val expectedError: String
) {
  def this(endpointParam: CommentAuthEndpointParameters, headers: Map[String, String], expectedError: String) = {
    this(endpointParam.endpoint, endpointParam.request, headers, expectedError)
  }
}

case class CommentAuthEndpointParameters(
    endpoint: ZIO[CommentsServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    request: Request[Either[String, String], Any]
)
object CommentAuthEndpointParameters:
  def addComment(slug: String): CommentAuthEndpointParameters = CommentAuthEndpointParameters(
    endpoint = ZIO.service[CommentsServerEndpoints].map(endpoints => endpoints.addCommentServerEndpoint),
    request = basicRequest.post(uri"http://test.com/api/articles/$slug/comments")
  )
  def deleteComment(slug: String, commentId: Int): CommentAuthEndpointParameters = CommentAuthEndpointParameters(
    endpoint = ZIO.service[CommentsServerEndpoints].map(endpoints => endpoints.deleteCommentServerEndpoint),
    request = basicRequest.delete(uri"http://test.com/api/articles/$slug/comments/$commentId")
  )
  def getCommentsFromArticle(slug: String): CommentAuthEndpointParameters = CommentAuthEndpointParameters(
    endpoint = ZIO.service[CommentsServerEndpoints].map(endpoints => endpoints.getCommentsFromArticleServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/articles/$slug/comments")
  )
