package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

object UserLogin:
  given userLoginEncoder: zio.json.JsonEncoder[UserLogin] = DeriveJsonEncoder.gen[UserLogin]
  given userLoginDecoder: zio.json.JsonDecoder[UserLogin] = DeriveJsonDecoder.gen[UserLogin]

case class UserLogin(
    user: UserLoginData
)
