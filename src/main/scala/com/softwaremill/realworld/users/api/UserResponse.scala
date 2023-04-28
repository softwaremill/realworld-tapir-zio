package com.softwaremill.realworld.users.api

import com.softwaremill.realworld.users.User
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserResponse(
    user: User
)

object UserResponse:
  given userEncoder: zio.json.JsonEncoder[UserResponse] = DeriveJsonEncoder.gen[UserResponse]
  given userDecoder: zio.json.JsonDecoder[UserResponse] = DeriveJsonDecoder.gen[UserResponse]
