package com.softwaremill.realworld.users.model

case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: Option[String],
    image: Option[String]
)
