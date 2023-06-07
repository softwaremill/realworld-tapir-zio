package com.softwaremill.realworld.users.domain

import sttp.tapir.{Schema, SchemaType}
import zio.json.{JsonDecoder, JsonEncoder}

case class Username(value: String) extends AnyVal

object Username:
  given profileUsernameEncoder: JsonEncoder[Username] = JsonEncoder[String].contramap((p: Username) => p.value)
  given profileUsernameDecoder: JsonDecoder[Username] = JsonDecoder[String].map(username => Username(username))
  given profileUsernameSchema: Schema[Username] = Schema(SchemaType.SString())
