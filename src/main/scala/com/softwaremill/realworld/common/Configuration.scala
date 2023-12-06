package com.softwaremill.realworld.common

import zio.ZLayer
import zio._
import zio.config.magnolia._

final case class RootConfig(
    config: AppConfig
)
final case class AppConfig(
    system: SystemConfig,
    db: DbConfig
)

final case class SystemConfig(
    jwtSecret: String
)

final case class DbConfig(
    url: String
)

object Configuration:
  import zio.config.typesafe.*

  val live: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer.fromZIO(TypesafeConfigProvider.fromResourcePath().load(deriveConfig[RootConfig]).map(_.config))
