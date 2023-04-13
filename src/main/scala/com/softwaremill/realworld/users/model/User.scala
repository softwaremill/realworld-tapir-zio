package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

object User:
  given userEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]

case class User(
    user: UserData
)
