package com.softwaremill.realworld.profiles

import zio.ZIO
import com.softwaremill.realworld.users.{UserRegisterData, UsersRepository}

object ProfileRepositoryTestSupport {

  private val exampleUser1 = UserRegisterData(
    email = "jake@example.com",
    username = "jake",
    password = "secret password"
  )

  private val exampleUser2 = UserRegisterData(
    email = "john@example.com",
    username = "john",
    password = "secret password"
  )

  def prepareBasicProfileData = {
    for {
      userRepo <- ZIO.service[UsersRepository]
      profileRepo <- ZIO.service[ProfilesRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      user1 <- userRepo.findByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      user2 <- userRepo.findByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- profileRepo.follow(user1.userId, user2.userId)
    } yield ()
  }
}
