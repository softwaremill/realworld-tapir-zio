package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserRegister(
    user: UserRegisterData
)

object UserRegister:
  given userRegisterEncoder: zio.json.JsonEncoder[UserRegister] = DeriveJsonEncoder.gen[UserRegister]
  given userRegisterDecoder: zio.json.JsonDecoder[UserRegister] = DeriveJsonDecoder.gen[UserRegister]
