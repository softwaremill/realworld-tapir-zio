package com.softwaremill.realworld.users.api

import com.softwaremill.realworld.common.domain.Username
import com.softwaremill.realworld.common.{BaseEndpoints, ErrorInfo, UserSession}
import com.softwaremill.realworld.users.{Email, Profile, User}
import sttp.tapir.Endpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir.*
import zio.ZLayer

import scala.util.chaining.*

class UsersEndpoints(base: BaseEndpoints):

  val registerEndpoint: Endpoint[Unit, UserRegisterRequest, ErrorInfo, UserResponse, Any] = base.publicEndpoint.post
    .in("api" / "users")
    .in(jsonBody[UserRegisterRequest].example(Examples.userRegisterRequest))
    .out(jsonBody[UserResponse].example(Examples.userResponse()))

  val loginEndpoint: Endpoint[Unit, UserLoginRequest, ErrorInfo, UserResponse, Any] = base.publicEndpoint.post
    .in("api" / "users" / "login")
    .in(jsonBody[UserLoginRequest].example(Examples.userLoginRequest))
    .out(jsonBody[UserResponse].example(Examples.userResponse()))

  val getCurrentUserEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Unit, ErrorInfo, UserResponse, Any] = base.secureEndpoint.get
    .in("api" / "user")
    .out(jsonBody[UserResponse].example(Examples.getUserResponse))

  val updateEndpoint: ZPartialServerEndpoint[Any, String, UserSession, UserUpdateRequest, ErrorInfo, UserResponse, Any] =
    base.secureEndpoint.put
      .in("api" / "user")
      .in(jsonBody[UserUpdateRequest].example(Examples.userUpdateRequest))
      .out(jsonBody[UserResponse].example(Examples.updatedUserResponse))

  val getProfileEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Username, ErrorInfo, ProfileResponse, Any] =
    base.secureEndpoint.get
      .in("api" / "profiles" / path[Username]("username"))
      .out(jsonBody[ProfileResponse].example(Examples.profileResponse))

  val followUserEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Username, ErrorInfo, ProfileResponse, Any] =
    base.secureEndpoint.post
      .in("api" / "profiles" / path[Username]("username") / "follow")
      .out(jsonBody[ProfileResponse].example(Examples.profileResponse))

  val unfollowUserEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Username, ErrorInfo, ProfileResponse, Any] =
    base.secureEndpoint.delete
      .in("api" / "profiles" / path[Username]("username") / "follow")
      .out(jsonBody[ProfileResponse].example(Examples.profileResponse))

  private object Examples:
    val userRegisterRequest: UserRegisterRequest = UserRegisterRequest(user =
      UserRegisterData(
        email = "user123@email.com",
        username = "user123",
        password = "secret_password"
      )
    )

    val userLoginRequest: UserLoginRequest = UserLoginRequest(user =
      UserLoginData(
        email = "user123@email.com",
        password = "secret_password"
      )
    )

    val userUpdateRequest: UserUpdateRequest = UserUpdateRequest(user =
      UserUpdateData(
        email = Some("updatedUser@email.com"),
        username = Some("updatedUser"),
        password = Some("invalid_password"),
        bio = Some("updatedUserBio"),
        image = Some("updatedImageBio")
      )
    )

    def userResponse(
        email: String = "user123@email.com",
        token: Option[String] = Some(
          "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJTb2Z0d2FyZU1pbGwiLCJ1c2VyRW1haWwiOiJ1c2VyMTIzQGVtYWlsLmNvbSIsImV4cCI6MTY4MjU4MzY0NCwiaWF0IjoxNjgyNTgwMDQ0LCJqdGkiOiJkMmEzYThjZC1mNmFhLTQwNzgtYTk4Ni1jZmIwNTg5NTAxYmEifQ.SwY-ynkmR3-uYZU0K2cI0NY7Cs8oSgCU8RUVUagOAok"
        ),
        username: String = "user123",
        bio: Option[String] = None,
        image: Option[String] = None
    ): UserResponse =
      UserResponse(user =
        User(
          email = Email(email),
          token = token,
          username = Username(username),
          bio = bio,
          image = image
        )
      )

    val getUserResponse: UserResponse = userResponse(token = None, bio = Some("userBio"), image = Some("userImage"))

    val updatedUserResponse: UserResponse = userResponse(
      email = "updatedUser@email.com",
      token = None,
      username = "updatedUser",
      bio = Some("updatedUserBio"),
      image = Some("updatedImageBio")
    )

    val profileResponse: ProfileResponse = ProfileResponse(profile =
      Profile(
        username = Username("user123"),
        bio = Some("userBio"),
        image = Some("userImage"),
        following = false
      )
    )

object UsersEndpoints:
  val live: ZLayer[BaseEndpoints, Nothing, UsersEndpoints] = ZLayer.fromFunction(new UsersEndpoints(_))
