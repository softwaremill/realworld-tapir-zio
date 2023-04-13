package com.softwaremill.realworld.users.model

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

object UserUpdate:
  given userUpdateEncoder: zio.json.JsonEncoder[UserUpdate] = DeriveJsonEncoder.gen[UserUpdate]
  given userUpdateDecoder: zio.json.JsonDecoder[UserUpdate] = DeriveJsonDecoder.gen[UserUpdate]

case class UserUpdate(
    user: UserUpdateData
)
