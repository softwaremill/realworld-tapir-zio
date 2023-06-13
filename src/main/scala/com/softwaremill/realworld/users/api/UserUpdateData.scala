package com.softwaremill.realworld.users.api

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.users.UserWithPassword
import sttp.tapir.Schema.annotations.validateEach
import sttp.tapir.Validator
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

case class UserUpdateData(
    @validateEach(Validator.nonEmptyString) email: Option[String],
    @validateEach(Validator.minLength(3)) username: Option[String],
    @validateEach(Validator.nonEmptyString) password: Option[String],
    @validateEach(Validator.nonEmptyString) bio: Option[String],
    @validateEach(Validator.nonEmptyString) image: Option[String]
) {
  def update(userDataWithPassword: UserWithPassword): UserUpdateData =
    val user = userDataWithPassword.user
    UserUpdateData(
      this.email.orElse(Some(user.email.value)),
      this.username.orElse(Some(user.username.value)),
      Some(userDataWithPassword.hashedPassword),
      this.bio.orElse(user.bio),
      this.image.orElse(user.image)
    )
}

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
