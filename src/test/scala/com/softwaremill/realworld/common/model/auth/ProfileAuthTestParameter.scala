package com.softwaremill.realworld.common.model.auth

import com.softwaremill.realworld.profiles.ProfilesEndpoints
import sttp.client3.{Request, UriContext, basicRequest}
import sttp.model.Uri
import sttp.tapir.ztapir.ZServerEndpoint
import zio.ZIO

class ProfileAuthTestParameters(
    val endpoint: ZIO[ProfilesEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    val request: Request[Either[String, String], Any],
    val headers: Map[String, String],
    val expectedError: String
):
  def this(endpointParam: ProfileAuthEndpointParameters, headers: Map[String, String], expectedError: String) = {
    this(endpointParam.endpoint, endpointParam.request, headers, expectedError)
  }

case class ProfileAuthEndpointParameters(
    endpoint: ZIO[ProfilesEndpoints, Nothing, ZServerEndpoint[Any, Any]],
    request: Request[Either[String, String], Any]
)

object ProfileAuthEndpointParameters:
  def getProfile(username: String): ProfileAuthEndpointParameters = ProfileAuthEndpointParameters(
    endpoint = ZIO.service[ProfilesEndpoints].map(endpoints => endpoints.getProfile),
    request = basicRequest.get(uri"http://test.com/api/profiles/$username")
  )
  def followUser(username: String): ProfileAuthEndpointParameters = ProfileAuthEndpointParameters(
    endpoint = ZIO.service[ProfilesEndpoints].map(endpoints => endpoints.followUser),
    request = basicRequest.post(uri"http://test.com/api/profiles/$username/follow")
  )
  def unfollowUser(username: String): ProfileAuthEndpointParameters = ProfileAuthEndpointParameters(
    endpoint = ZIO.service[ProfilesEndpoints].map(endpoints => endpoints.unfollowUser),
    request = basicRequest.delete(uri"http://test.com/api/profiles/$username/follow")
  )
