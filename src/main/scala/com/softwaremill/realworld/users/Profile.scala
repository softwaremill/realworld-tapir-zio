package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.NoneAsNullOptionEncoder.*
import com.softwaremill.realworld.common.domain.Username
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Profile(username: Username, bio: Option[String], image: Option[String], following: Boolean)

object Profile:
  given profileDataEncoder: JsonEncoder[Profile] = DeriveJsonEncoder.gen[Profile]
  given profileDataDecoder: JsonDecoder[Profile] = DeriveJsonDecoder.gen[Profile]
