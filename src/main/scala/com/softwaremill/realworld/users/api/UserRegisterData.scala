package com.softwaremill.realworld.users.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserRegisterData(
    email: String,
    username: String,
    password: String
)

object UserRegisterData:
  given userRegisterDataEncoder: zio.json.JsonEncoder[UserRegisterData] = DeriveJsonEncoder.gen[UserRegisterData]
  given userRegisterDataDecoder: zio.json.JsonDecoder[UserRegisterData] = DeriveJsonDecoder.gen[UserRegisterData]
