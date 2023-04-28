package com.softwaremill.realworld.common.model.auth

import com.softwaremill.realworld.articles.core.ArticlesServerEndpoints
import sttp.client3.{Request, UriContext, basicRequest}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO

class ArticleAuthTestParameters(
    val endpoint: ZIO[ArticlesServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    val request: Request[Either[String, String], Any],
    val headers: Map[String, String],
    val expectedError: String
) {
  def this(endpointParam: ArticleAuthEndpointParameters, headers: Map[String, String], expectedError: String) = {
    this(endpointParam.endpoint, endpointParam.request, headers, expectedError)
  }
}

case class ArticleAuthEndpointParameters(
    endpoint: ZIO[ArticlesServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    request: Request[Either[String, String], Any]
)
object ArticleAuthEndpointParameters:
  def listArticles: ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.listArticlesServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/articles")
  )
  def feedArticles: ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.feedArticlesServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/articles/feed")
  )
  def get(slug: String): ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.getServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/articles/$slug")
  )
  def create: ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.createServerEndpoint),
    request = basicRequest.post(uri"http://test.com/api/articles")
  )
  def delete(slug: String): ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.deleteServerEndpoint),
    request = basicRequest.delete(uri"http://test.com/api/articles/$slug")
  )
  def update(slug: String): ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.updateServerEndpoint),
    request = basicRequest.put(uri"http://test.com/api/articles/$slug")
  )
  def makeFavorite(slug: String): ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.makeFavoriteServerEndpoint),
    request = basicRequest.post(uri"http://test.com/api/articles/$slug/favorite")
  )
  def removeFavorite(slug: String): ArticleAuthEndpointParameters = ArticleAuthEndpointParameters(
    endpoint = ZIO.service[ArticlesServerEndpoints].map(endpoints => endpoints.removeFavoriteServerEndpoint),
    request = basicRequest.delete(uri"http://test.com/api/articles/$slug/favorite")
  )
