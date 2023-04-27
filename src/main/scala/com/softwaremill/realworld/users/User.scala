package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.common.db.UserRow
import com.softwaremill.realworld.users.api.UserUpdateData
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class User(
    email: String,
    token: Option[String],
    username: String,
    bio: Option[String],
    image: Option[String]
)

object User:

  def fromRow(userRow: UserRow): User = User(
    userRow.email,
    None,
    userRow.username,
    userRow.bio,
    userRow.image
  )

  def fromUpdate(userUpdateData: UserUpdateData): User = User(
    email = userUpdateData.email.orNull,
    token = Option.empty[String],
    username = userUpdateData.username.orNull,
    bio = userUpdateData.bio,
    image = userUpdateData.image
  )

  given userDataEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  given userDataDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]
