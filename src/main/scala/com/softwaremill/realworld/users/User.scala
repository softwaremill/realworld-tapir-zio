package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.common.domain.Username
import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Email(value: String) extends AnyVal
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

object Email:
  given emailEncoder: JsonEncoder[Email] = JsonEncoder[String].contramap((c: Email) => c.value)
  given emailDecoder: JsonDecoder[Email] = JsonDecoder[String].map(email => Email(email))
  given emailSchema: Schema[Email] = Schema(SchemaType.SString())
