package com.softwaremill.realworld.users.api

import com.softwaremill.realworld.users.Profile
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ProfileResponse(profile: Profile)

object ProfileResponse:
  given profileEncoder: JsonEncoder[ProfileResponse] = DeriveJsonEncoder.gen[ProfileResponse]
  given profileDecoder: JsonDecoder[ProfileResponse] = DeriveJsonDecoder.gen[ProfileResponse]
