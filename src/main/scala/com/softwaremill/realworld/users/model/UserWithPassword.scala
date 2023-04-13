package com.softwaremill.realworld.users.model

case class UserWithPassword(
    user: UserData,
    hashedPassword: String
)
