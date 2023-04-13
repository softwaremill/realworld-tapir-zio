package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserData(
    email: String,
    token: Option[String],
    username: String,
    bio: Option[String],
    image: Option[String]
)

object UserData:
  given userDataEncoder: zio.json.JsonEncoder[UserData] = DeriveJsonEncoder.gen[UserData]
  given userDataDecoder: zio.json.JsonDecoder[UserData] = DeriveJsonDecoder.gen[UserData]
