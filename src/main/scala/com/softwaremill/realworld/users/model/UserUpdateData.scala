package com.softwaremill.realworld.users.model

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserUpdateData(
    email: Option[String],
    username: Option[String],
    password: Option[String],
    bio: Option[String],
    image: Option[String]
)

object UserUpdateData:
  def apply(
      email: Option[String],
      username: Option[String],
      password: Option[String],
      bio: Option[String],
      image: Option[String]
  ): UserUpdateData = {
    new UserUpdateData(
      email.map(_.toLowerCase.trim),
      username.map(_.trim),
      password,
      bio,
      image
    )
  }
  given userUpdateDataEncoder: zio.json.JsonEncoder[UserUpdateData] = DeriveJsonEncoder.gen[UserUpdateData]
  given userUpdateDataDecoder: zio.json.JsonDecoder[UserUpdateData] = DeriveJsonDecoder.gen[UserUpdateData]
