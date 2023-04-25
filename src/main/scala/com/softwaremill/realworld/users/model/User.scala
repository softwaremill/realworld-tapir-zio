package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class User(
    user: UserData
)

object User:
  given userEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]
