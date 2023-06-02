package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import sttp.tapir.{Schema, SchemaType}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ProfileUsername(value: String) extends AnyVal
case class Profile(username: ProfileUsername, bio: Option[String], image: Option[String], following: Boolean)

object Profile:
  given profileDataEncoder: JsonEncoder[Profile] = DeriveJsonEncoder.gen[Profile]
  given profileDataDecoder: JsonDecoder[Profile] = DeriveJsonDecoder.gen[Profile]

object ProfileUsername:
  given profileUsernameEncoder: JsonEncoder[ProfileUsername] = JsonEncoder[String].contramap((p: ProfileUsername) => p.value)
  given profileUsernameDecoder: JsonDecoder[ProfileUsername] = JsonDecoder[String].map(username => ProfileUsername(username))
  given profileUsernameSchema: Schema[ProfileUsername] = Schema(SchemaType.SString())
