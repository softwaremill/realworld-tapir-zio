package com.softwaremill.realworld.users.model

case class UserWithPassword(
    user: UserData,
    hashedPassword: String
)

object UserWithPassword {
  def fromRow(userRow: UserRow): UserWithPassword = {
    UserWithPassword(
      UserData.fromRow(userRow),
      userRow.password
    )
  }
}
