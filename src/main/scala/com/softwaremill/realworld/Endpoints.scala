package com.softwaremill.realworld

import com.softwaremill.realworld.articles.core.ArticlesEndpoints
import com.softwaremill.realworld.articles.tags.TagsEndpoints
import com.softwaremill.realworld.common.db.DbConfig
import com.softwaremill.realworld.users.UsersServerEndpoints
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, ZIO, ZLayer}

class Endpoints(
    articlesEndpoints: ArticlesEndpoints,
    usersEndpoints: UsersServerEndpoints,
    tagsEndpoints: TagsEndpoints
):

  val endpoints: List[ZServerEndpoint[Any, Any]] = {
    val api = articlesEndpoints.endpoints ++ usersEndpoints.endpoints ++ tagsEndpoints.endpoints
    val docs = docsEndpoints(api)
    api ++ docs
  }

  private def docsEndpoints(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](apiEndpoints, "realworld-tapir-zio", "0.1.0")

object Endpoints:
  val live: ZLayer[ArticlesEndpoints with UsersServerEndpoints with TagsEndpoints, Nothing, Endpoints] =
    ZLayer.fromFunction(new Endpoints(_, _, _))
