package com.softwaremill.realworld.users.api

import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserLoginData(
    @validate(Validator.nonEmptyString) email: String,
    @validate(Validator.nonEmptyString) password: String
)

object UserLoginData:
  given userLoginDataEncoder: zio.json.JsonEncoder[UserLoginData] = DeriveJsonEncoder.gen[UserLoginData]
  given userLoginDataDecoder: zio.json.JsonDecoder[UserLoginData] = DeriveJsonDecoder.gen[UserLoginData]
