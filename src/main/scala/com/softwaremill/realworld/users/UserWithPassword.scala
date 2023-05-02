package com.softwaremill.realworld.users

import com.softwaremill.realworld.users.User

case class UserWithPassword(
    user: User,
    hashedPassword: String
)
