package com.softwaremill.realworld.articles.tags

import com.softwaremill.realworld.articles.tags.api.{TagsEndpoints, TagsListResponse}
import com.softwaremill.realworld.common.ErrorMapper.defaultErrorsMappings
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class TagsServerEndpoints(tagsEndpoints: TagsEndpoints, tagsService: TagsService):

  val getTagsServerEndpoint: ZServerEndpoint[Any, Any] = tagsEndpoints.getTagsEndpoint
    .zServerLogic(_ =>
      tagsService.getAllTags
        .map(foundTags => TagsListResponse(tags = foundTags))
        .logError
        .pipe(defaultErrorsMappings)
    )

  val endpoints: List[ZServerEndpoint[Any, Any]] =
    List(getTagsServerEndpoint)

object TagsServerEndpoints:
  val live: ZLayer[TagsEndpoints with TagsService, Nothing, TagsServerEndpoints] = ZLayer.fromFunction(new TagsServerEndpoints(_, _))
