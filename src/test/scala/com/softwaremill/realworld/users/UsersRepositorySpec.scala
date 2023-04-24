package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.Pagination
import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.UserRepositoryTestSupport.*
import com.softwaremill.realworld.utils.TestUtils.*
import zio.test.ZIOSpecDefault

import java.time.{Instant, ZonedDateTime}
import javax.sql.DataSource

object UsersRepositorySpec extends ZIOSpecDefault:
  def spec = suite("check user repository features")(
    suite("find user by email")(
      test("check user not found") {
        checkUserNotFound("notExisting@example.com")
      },
      test("check user found") {
        checkUserFound("admin@example.com")
      }
    ),
    suite("find user with password by email")(
      test("check user with password found") {
        checkUserWithPasswordFound("admin@example.com")
      },
      test("check user with password not found") {
        checkUserWithPasswordNotFound("notExisting@example.com")
      }
    ),
    suite("add user")(
      test("check user added") {
        checkUserAdd(UserRegisterData(email = "test@test.com", username = "tested", password = "tested"))
      }
    )
  ).provide(
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
