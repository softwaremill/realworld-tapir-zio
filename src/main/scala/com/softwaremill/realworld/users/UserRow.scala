package com.softwaremill.realworld.users

//TODO Rows only in repositories, probably make copy to Article
case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: Option[String],
    image: Option[String]
)
