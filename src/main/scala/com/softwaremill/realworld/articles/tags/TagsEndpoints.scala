package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.common.BaseEndpoints
import com.softwaremill.realworld.http.ErrorMapper.defaultErrorsMappings
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class TagsEndpoints(tagsService: TagsService, base: BaseEndpoints):

  val getTags: ZServerEndpoint[Any, Any] = base.publicEndpoint.get
    .in("api" / "tags")
    .out(jsonBody[TagsList])
    .zServerLogic(_ =>
      tagsService.getAllTags
        .map(foundTags => TagsList(tags = foundTags.map(_.tag)))
        .logError
        .pipe(defaultErrorsMappings)
    )

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    List(getTags)

object TagsEndpoints:
  val live: ZLayer[TagsService with BaseEndpoints, Nothing, TagsEndpoints] = ZLayer.fromFunction(new TagsEndpoints(_, _))
