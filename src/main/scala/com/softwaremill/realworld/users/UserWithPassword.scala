package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.db.UserRow
import com.softwaremill.realworld.users.User

case class UserWithPassword(
    user: User,
    hashedPassword: String
)

object UserWithPassword {
  def fromRow(userRow: UserRow): UserWithPassword = {
    UserWithPassword(
      User.fromRow(userRow),
      userRow.password
    )
  }
}
