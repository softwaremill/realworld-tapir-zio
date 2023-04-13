package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserLoginData(
    email: String,
    password: String
)

object UserLoginData:
  given userLoginDataEncoder: zio.json.JsonEncoder[UserLoginData] = DeriveJsonEncoder.gen[UserLoginData]
  given userLoginDataDecoder: zio.json.JsonDecoder[UserLoginData] = DeriveJsonDecoder.gen[UserLoginData]
