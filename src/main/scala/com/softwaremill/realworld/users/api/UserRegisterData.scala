package com.softwaremill.realworld.users.api

import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserRegisterData(
    @validate(Validator.nonEmptyString) email: String,
    @validate(Validator.minLength(3)) username: String,
    @validate(Validator.nonEmptyString) password: String
)

object UserRegisterData:
  given userRegisterDataEncoder: zio.json.JsonEncoder[UserRegisterData] = DeriveJsonEncoder.gen[UserRegisterData]
  given userRegisterDataDecoder: zio.json.JsonDecoder[UserRegisterData] = DeriveJsonDecoder.gen[UserRegisterData]
