package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class User(
    email: String,
    token: Option[String],
    username: String,
    bio: Option[String],
    image: Option[String]
)

object User:
  given userDataEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDataDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]
