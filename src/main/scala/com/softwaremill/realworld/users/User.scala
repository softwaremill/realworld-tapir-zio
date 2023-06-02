package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class UserEmail(value: String) extends AnyVal
case class UserUsername(value: String) extends AnyVal
case class User(
    email: UserEmail,
    token: Option[String],
    username: UserUsername,
    bio: Option[String],
    image: Option[String]
)

object User:
  given userDataEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDataDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]

object UserEmail:
  given userEmailEncoder: JsonEncoder[UserEmail] = JsonEncoder[String].contramap((c: UserEmail) => c.value)
  given userEmailDecoder: JsonDecoder[UserEmail] = JsonDecoder[String].map(email => UserEmail(email))
  given userEmailSchema: Schema[UserEmail] = Schema(SchemaType.SString())

object UserUsername:
  given userEmailEncoder: JsonEncoder[UserUsername] = JsonEncoder[String].contramap((c: UserUsername) => c.value)
  given userEmailDecoder: JsonDecoder[UserUsername] = JsonDecoder[String].map(email => UserUsername(email))
  given userEmailSchema: Schema[UserUsername] = Schema(SchemaType.SString())
