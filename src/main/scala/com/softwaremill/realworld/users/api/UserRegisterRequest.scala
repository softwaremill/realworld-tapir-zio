package com.softwaremill.realworld.users.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserRegisterRequest(
    user: UserRegisterData
)

object UserRegisterRequest:
  given userRegisterEncoder: zio.json.JsonEncoder[UserRegisterRequest] = DeriveJsonEncoder.gen[UserRegisterRequest]
  given userRegisterDecoder: zio.json.JsonDecoder[UserRegisterRequest] = DeriveJsonDecoder.gen[UserRegisterRequest]
