package com.softwaremill.realworld.users

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.Instant

case class User(
    user: UserData
)
object User {
  implicit val userEncoder: zio.json.JsonEncoder[User] = DeriveJsonEncoder.gen[User]
  implicit val userDecoder: zio.json.JsonDecoder[User] = DeriveJsonDecoder.gen[User]
}

case class UserRegister(
    user: UserRegisterData
)
object UserRegister {
  implicit val userRegisterRequestBodyEncoder: zio.json.JsonEncoder[UserRegister] = DeriveJsonEncoder.gen[UserRegister]
  implicit val userRegisterRequestBodyDecoder: zio.json.JsonDecoder[UserRegister] = DeriveJsonDecoder.gen[UserRegister]
}

case class UserLogin(
    user: UserLoginData
)
object UserLogin {
  implicit val userLoginRequestBodyEncoder: zio.json.JsonEncoder[UserLogin] = DeriveJsonEncoder.gen[UserLogin]
  implicit val userLoginRequestBodyDecoder: zio.json.JsonDecoder[UserLogin] = DeriveJsonDecoder.gen[UserLogin]
}

case class UserData(
    email: String,
    token: Option[String],
    username: String,
    bio: Option[String],
    image: Option[String]
)
object UserData {
  implicit val userDataEncoder: zio.json.JsonEncoder[UserData] = DeriveJsonEncoder.gen[UserData]
  implicit val userDataDecoder: zio.json.JsonDecoder[UserData] = DeriveJsonDecoder.gen[UserData]
}

case class UserRegisterData(
    email: String,
    username: String,
    password: String
)
object UserRegisterData {
  implicit val userRegisterDataEncoder: zio.json.JsonEncoder[UserRegisterData] = DeriveJsonEncoder.gen[UserRegisterData]
  implicit val userRegisterDataDecoder: zio.json.JsonDecoder[UserRegisterData] = DeriveJsonDecoder.gen[UserRegisterData]
}

case class UserLoginData(
    email: String,
    password: String
)
object UserLoginData {
  implicit val userLoginDataEncoder: zio.json.JsonEncoder[UserLoginData] = DeriveJsonEncoder.gen[UserLoginData]
  implicit val userLoginDataDecoder: zio.json.JsonDecoder[UserLoginData] = DeriveJsonDecoder.gen[UserLoginData]
}

case class UserWithPassword(
    user: UserData,
    hashedPassword: String
)

case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: String,
    image: String
)

case class UserSession(email: String)
