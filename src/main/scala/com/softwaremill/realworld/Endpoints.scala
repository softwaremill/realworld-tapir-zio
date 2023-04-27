package com.softwaremill.realworld

import com.softwaremill.realworld.articles.comments.CommentsServerEndpoints
import com.softwaremill.realworld.articles.core.ArticlesEndpoints
import com.softwaremill.realworld.articles.tags.TagsEndpoints
import com.softwaremill.realworld.common.db.DbConfig
import com.softwaremill.realworld.users.UsersServerEndpoints
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, ZIO, ZLayer}

class Endpoints(
    articlesEndpoints: ArticlesEndpoints,
    commentsServerEndpoints: CommentsServerEndpoints,
    tagsEndpoints: TagsEndpoints,
    usersServerEndpoints: UsersServerEndpoints
):

  val endpoints: List[ZServerEndpoint[Any, Any]] = {
    val api = articlesEndpoints.endpoints ++ commentsServerEndpoints.endpoints ++ tagsEndpoints.endpoints ++ usersServerEndpoints.endpoints
    val docs = docsEndpoints(api)
    api ++ docs
  }

  private def docsEndpoints(apiEndpoints: List[ZServerEndpoint[Any, Any]]): List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
    .fromServerEndpoints[Task](apiEndpoints, "realworld-tapir-zio", "0.1.0")

object Endpoints:
  val live: ZLayer[ArticlesEndpoints with CommentsServerEndpoints with TagsEndpoints with UsersServerEndpoints, Nothing, Endpoints] =
    ZLayer.fromFunction(new Endpoints(_, _, _, _))
