package com.softwaremill.realworld.common.model.auth

import com.softwaremill.realworld.users.UsersServerEndpoints
import sttp.client3.{Request, UriContext, basicRequest}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO

class UserAuthTestParameters(
    val endpoint: ZIO[UsersServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    val request: Request[Either[String, String], Any],
    val headers: Map[String, String],
    val expectedError: String
):
  def this(endpointParam: UserAuthEndpointParameters, headers: Map[String, String], expectedError: String) = {
    this(endpointParam.endpoint, endpointParam.request, headers, expectedError)
  }

case class UserAuthEndpointParameters(
    endpoint: ZIO[UsersServerEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    request: Request[Either[String, String], Any]
)
object UserAuthEndpointParameters:
  val getCurrentUser: UserAuthEndpointParameters = UserAuthEndpointParameters(
    endpoint = ZIO.service[UsersServerEndpoints].map(endpoints => endpoints.getCurrentUserServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/user")
  )
  val update: UserAuthEndpointParameters = UserAuthEndpointParameters(
    endpoint = ZIO.service[UsersServerEndpoints].map(endpoints => endpoints.updateServerEndpoint),
    request = basicRequest.put(uri"http://test.com/api/user")
  )
  def getProfile(username: String): UserAuthEndpointParameters = UserAuthEndpointParameters(
    endpoint = ZIO.service[UsersServerEndpoints].map(endpoints => endpoints.getProfileServerEndpoint),
    request = basicRequest.get(uri"http://test.com/api/profiles/$username")
  )
  def followUser(username: String): UserAuthEndpointParameters = UserAuthEndpointParameters(
    endpoint = ZIO.service[UsersServerEndpoints].map(endpoints => endpoints.followUserServerEndpoint),
    request = basicRequest.post(uri"http://test.com/api/profiles/$username/follow")
  )
  def unfollowUser(username: String): UserAuthEndpointParameters = UserAuthEndpointParameters(
    endpoint = ZIO.service[UsersServerEndpoints].map(endpoints => endpoints.unfollowUserServerEndpoint),
    request = basicRequest.delete(uri"http://test.com/api/profiles/$username/follow")
  )
