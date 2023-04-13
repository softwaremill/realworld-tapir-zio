package com.softwaremill.realworld.users

import com.softwaremill.realworld.users.model.{UserData, UserRow, UserWithPassword, UserUpdateData}

object UserMapper: // TODO probably this mapper can be replaced with something better in Scala

  def toUserData(userRow: UserRow): UserData = UserData(
    userRow.email,
    Option.empty[String],
    userRow.username,
    userRow.bio,
    userRow.image
  )
