package com.softwaremill.realworld.users.model

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserData(
    email: String,
    token: Option[String],
    username: String,
    bio: Option[String],
    image: Option[String]
)

object UserData:

  def fromRow(userRow: UserRow): UserData = UserData(
    userRow.email,
    None,
    userRow.username,
    userRow.bio,
    userRow.image
  )

  def fromUpdate(userUpdateData: UserUpdateData): UserData = UserData(
    email = userUpdateData.email.orNull,
    token = Option.empty[String],
    username = userUpdateData.username.orNull,
    bio = userUpdateData.bio,
    image = userUpdateData.image
  )

  given userDataEncoder: zio.json.JsonEncoder[UserData] = DeriveJsonEncoder.gen[UserData]
  given userDataDecoder: zio.json.JsonDecoder[UserData] = DeriveJsonDecoder.gen[UserData]
