package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.common.domain.Username
import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class User(
    email: Email,
    token: Option[String],
    username: Username,
    bio: Option[String],
    image: Option[String]
)

object User:
  given userDataEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDataDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]
