package com.softwaremill.realworld.users

import com.softwaremill.realworld.utils.DbData.*
import zio.ZIO
object UserDbTestSupport:
  def prepareOneUser =
    ZIO.service[UsersRepository].flatMap(_.add(exampleUser1))

  def prepareTwoUsers =
    for {
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
    } yield ()

  def prepareTwoUsersWithFollowing =
    for {
      userRepo <- ZIO.service[UsersRepository]
      _ <- userRepo.add(exampleUser1)
      _ <- userRepo.add(exampleUser2)
      userId1 <- userRepo.findUserIdByEmail(exampleUser1.email).someOrFail(s"User with email ${exampleUser1.email} doesn't exist.")
      userId2 <- userRepo.findUserIdByEmail(exampleUser2.email).someOrFail(s"User with email ${exampleUser2.email} doesn't exist.")
      _ <- userRepo.follow(userId1, userId2)
    } yield ()
