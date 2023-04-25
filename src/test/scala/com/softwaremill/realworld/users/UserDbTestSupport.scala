package com.softwaremill.realworld.users

import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO
object UserDbTestSupport:
  def prepareBasicUsersData = {
    for {
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
    } yield ()
  }
