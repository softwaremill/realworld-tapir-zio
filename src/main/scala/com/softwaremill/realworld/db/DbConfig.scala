package com.softwaremill.realworld.db

import com.softwaremill.realworld.common.AppConfig
import zio.{ZIO, ZLayer}

case class DbConfig(jdbcUrl: String):
  val connectionInitSql = "PRAGMA foreign_keys = ON"

object DbConfig:

  val live: ZLayer[AppConfig, Nothing, DbConfig] =
    ZLayer.fromZIO {
      for {
        appConfig <- ZIO.service[AppConfig]
      } yield DbConfig(appConfig.db.url)
    }
