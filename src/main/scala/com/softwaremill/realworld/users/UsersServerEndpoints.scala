package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.common.ErrorMapper.defaultErrorsMappings
import com.softwaremill.realworld.db.{Db, DbConfig}
import com.softwaremill.realworld.users.api.*
import io.getquill.SnakeCase
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.ztapir.*
import sttp.tapir.{EndpointInput, PublicEndpoint, Validator}
import zio.{Cause, Console, Exit, ZIO, ZLayer}

import javax.sql.DataSource
import scala.util.chaining.*

class UsersServerEndpoints(usersService: UsersService, userEndpoints: UsersEndpoints):

  val registerServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.registerEndpoint
    .zServerLogic(data =>
      usersService
        .register(data.user)
        .logError
        .pipe(defaultErrorsMappings)
    )

  val loginServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.loginEndpoint
    .zServerLogic(data =>
      usersService
        .login(data.user)
        .logError
        .pipe(defaultErrorsMappings)
        .map(UserResponse.apply)
    )

  val getCurrentUserServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.getCurrentUserEndpoint
    .serverLogic(session =>
      _ =>
        usersService
          .get(session.email)
          .logError
          .mapError {
            case e: Exceptions.NotFound => NotFound(e.message)
            case _                      => InternalServerError()
          }
          .map(UserResponse.apply)
    )

  val updateServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.updateEndpoint
    .serverLogic(session =>
      data =>
        usersService
          .update(data.user, session.email)
          .logError
          .pipe(defaultErrorsMappings)
          .map(UserResponse.apply)
    )

  val getProfileServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.getProfileEndpoint
    .serverLogic { session => username =>
      usersService
        .getProfile(username, session.email)
        .pipe(defaultErrorsMappings)
    }

  val followUserServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.followUserEndpoint
    .serverLogic { session => username =>
      usersService
        .follow(username, session.email)
        .pipe(defaultErrorsMappings)
    }

  val unfollowUserServerEndpoint: ZServerEndpoint[Any, Any] = userEndpoints.unfollowUserEndpoint
    .serverLogic { session => username =>
      usersService
        .unfollow(username, session.email)
        .pipe(defaultErrorsMappings)
    }

  val endpoints: List[ZServerEndpoint[Any, Any]] = List(
    getCurrentUserServerEndpoint,
    registerServerEndpoint,
    updateServerEndpoint,
    loginServerEndpoint,
    getProfileServerEndpoint,
    followUserServerEndpoint,
    unfollowUserServerEndpoint
  )

object UsersServerEndpoints:
  val live: ZLayer[UsersService with UsersEndpoints, Nothing, UsersServerEndpoints] =
    ZLayer.fromFunction(new UsersServerEndpoints(_, _))
