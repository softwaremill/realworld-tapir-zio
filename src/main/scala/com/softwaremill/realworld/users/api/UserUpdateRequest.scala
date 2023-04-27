package com.softwaremill.realworld.users.api

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserUpdateRequest(
    user: UserUpdateData
)

object UserUpdateRequest:
  given userUpdateEncoder: zio.json.JsonEncoder[UserUpdateRequest] = DeriveJsonEncoder.gen[UserUpdateRequest]
  given userUpdateDecoder: zio.json.JsonDecoder[UserUpdateRequest] = DeriveJsonDecoder.gen[UserUpdateRequest]
