package com.softwaremill.realworld.articles.tags

import zio.{ZIO, ZLayer}

import java.sql.SQLException

class TagsService(tagsRepository: TagsRepository):
  def getAllTags: ZIO[Any, SQLException, List[String]] = tagsRepository.listTags

object TagsService:
  val live: ZLayer[TagsRepository, Nothing, TagsService] = ZLayer.fromFunction(TagsService(_))
