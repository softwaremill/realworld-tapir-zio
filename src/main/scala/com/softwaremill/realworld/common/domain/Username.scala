package com.softwaremill.realworld.common.domain

import sttp.tapir.{Schema, SchemaType}
import zio.json.{JsonDecoder, JsonEncoder}

case class Username(value: String) extends AnyVal

object Username:
  given usernameEncoder: JsonEncoder[Username] = JsonEncoder[String].contramap((p: Username) => p.value)
  given usernameDecoder: JsonDecoder[Username] = JsonDecoder[String].map(username => Username(username))
  given usernameSchema: Schema[Username] = Schema(SchemaType.SString())
