package com.softwaremill.realworld.users

case class UserWithPassword(
    user: User,
    hashedPassword: String
)
