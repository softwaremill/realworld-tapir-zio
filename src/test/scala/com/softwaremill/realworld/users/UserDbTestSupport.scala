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

  def prepareBasicProfileData = {
    for {
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      user2 <- userRepo.findByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- userRepo.follow(user1.userId, user2.userId)
    } yield ()
  }
