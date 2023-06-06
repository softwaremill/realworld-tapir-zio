package com.softwaremill.realworld

import com.softwaremill.realworld.articles.comments.api.CommentsEndpoints
import com.softwaremill.realworld.articles.comments.{CommentsRepository, CommentsServerEndpoints, CommentsService}
import com.softwaremill.realworld.articles.core.api.ArticlesEndpoints
import com.softwaremill.realworld.articles.core.{ArticlesRepository, ArticlesServerEndpoints, ArticlesService}
import com.softwaremill.realworld.articles.tags.api.TagsEndpoints
import com.softwaremill.realworld.articles.tags.{TagsRepository, TagsServerEndpoints, TagsService}
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.api.UsersEndpoints
import com.softwaremill.realworld.users.{UsersRepository, UsersServerEndpoints, UsersService}
import sttp.tapir.server.ziohttp
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.*
import zio.logging.LogFormat
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault:

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = SLF4J.slf4j(LogFormat.colored)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =

    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)
    val options: ZioHttpServerOptions[Any] = ZioHttpServerOptions.customiseInterceptors
      .exceptionHandler(new DefectHandler())
      .decodeFailureHandler(CustomDecodeFailureHandler.create())
      .options

    (for
      migrator <- ZIO.service[DbMigrator]
      _ <- migrator.migrate()
      endpoints <- ZIO.service[Endpoints]
      httpApp = ZioHttpInterpreter(options).toHttp(endpoints.endpoints)
      actualPort <- Server.install(httpApp.withDefaultErrorResponse)
      _ <- Console.printLine(s"Application realworld-tapir-zio started")
      _ <- Console.printLine(s"Go to http://localhost:$actualPort/docs to open SwaggerUI")
      _ <- ZIO.never
    yield ())
      .provide(
        Configuration.live,
        DbConfig.live,
        Db.dataSourceLive,
        Db.quillLive,
        DbMigrator.live,
        Endpoints.live,
        AuthService.live,
        UsersEndpoints.live,
        UsersServerEndpoints.live,
        UsersService.live,
        UsersRepository.live,
        ArticlesEndpoints.live,
        ArticlesServerEndpoints.live,
        ArticlesService.live,
        ArticlesRepository.live,
        CommentsEndpoints.live,
        CommentsServerEndpoints.live,
        CommentsService.live,
        CommentsRepository.live,
        BaseEndpoints.live,
        TagsEndpoints.live,
        TagsServerEndpoints.live,
        TagsService.live,
        TagsRepository.live,
        Server.defaultWithPort(port)
      )
      .exitCode
