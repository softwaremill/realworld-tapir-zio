package com.softwaremill.realworld.users

import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.UserDbTestSupport.*
import com.softwaremill.realworld.users.UserRepositoryTestSupport.*
import com.softwaremill.realworld.utils.TestUtils.*
import zio.test.ZIOSpecDefault

import java.time.{Instant, ZonedDateTime}
import javax.sql.DataSource

object UsersRepositorySpec extends ZIOSpecDefault:
  def spec = suite("user repository tests")(
    suite("find user by email")(
      test("user not found") {
        for {
          _ <- prepareBasicUsersData
          result <- checkUserNotFound("notExisting@example.com")
        } yield result
      },
      test("user found") {
        for {
          _ <- prepareBasicUsersData
          result <- checkUserFound("jake@example.com")
        } yield result
      }
    ),
    suite("find user with password by email")(
      test("user with password found") {
        for {
          _ <- prepareBasicUsersData
          result <- checkUserWithPasswordFound("jake@example.com")
        } yield result
      },
      test("user with password not found") {
        for {
          _ <- prepareBasicUsersData
          result <- checkUserWithPasswordNotFound("notExisting@example.com")
        } yield result
      }
    ),
    suite("add user")(
      test("user added") {
        for {
          _ <- prepareBasicUsersData
          result <- checkUserAdd(UserRegisterData(email = "test@test.com", username = "tested", password = "tested"))
        } yield result
      }
    )
  ).provide(
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
