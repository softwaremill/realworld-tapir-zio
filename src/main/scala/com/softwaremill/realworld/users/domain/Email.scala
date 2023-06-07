package com.softwaremill.realworld.users.domain

import sttp.tapir.{Schema, SchemaType}
import zio.json.{JsonDecoder, JsonEncoder}

case class Email(value: String) extends AnyVal

object Email:
  given emailEncoder: JsonEncoder[Email] = JsonEncoder[String].contramap((c: Email) => c.value)
  given emailDecoder: JsonDecoder[Email] = JsonDecoder[String].map(email => Email(email))
  given emailSchema: Schema[Email] = Schema(SchemaType.SString())
