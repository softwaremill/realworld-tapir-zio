package com.softwaremill.realworld.users.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserLoginRequest(
    user: UserLoginData
)

object UserLoginRequest:
  given userLoginEncoder: zio.json.JsonEncoder[UserLoginRequest] = DeriveJsonEncoder.gen[UserLoginRequest]
  given userLoginDecoder: zio.json.JsonDecoder[UserLoginRequest] = DeriveJsonDecoder.gen[UserLoginRequest]
