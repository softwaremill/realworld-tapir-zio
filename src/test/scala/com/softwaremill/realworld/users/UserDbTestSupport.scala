package com.softwaremill.realworld.users

import com.softwaremill.realworld.utils.DbData.*
import com.softwaremill.realworld.utils.TestUtils.findUserIdByEmail
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
      userId1 <- findUserIdByEmail(userRepo, exampleUser1.email)
      userId2 <- findUserIdByEmail(userRepo, exampleUser2.email)
      _ <- userRepo.follow(userId1, userId2)
    } yield ()
