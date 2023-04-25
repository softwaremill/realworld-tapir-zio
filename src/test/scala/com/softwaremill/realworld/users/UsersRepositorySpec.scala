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
    ),
    suite("update user")(
      test("update user bio") {
        for {
          repo <- ZIO.service[UsersRepository]
          _ <- repo.add(UserRegisterData(email = "test@test.com", username = "tested", password = "tested"))
          user <- repo.updateByEmail(UserUpdateData(None, None, None, Some("Updated test bio"), None), "test@test.com")
        } yield zio.test.assert(user) {
          Assertion.isSome {
            Assertion.hasField[UserData, Option[String]](
              "bio",
              _.bio, {
                Assertion.isSome {
                  Assertion.equalTo("Updated test bio")
                }
              }
            ) && Assertion.hasField[UserData, String](
              "email",
              _.email, {
                Assertion.equalTo("test@test.com")
              }
            ) && Assertion.hasField[UserData, String](
              "username",
              _.username, {
                Assertion.equalTo("tested")
              }
            )
          }
        }
      },
      test("update user bio and email") {
        for {
          repo <- ZIO.service[UsersRepository]
          _ <- repo.add(UserRegisterData(email = "test@test.com", username = "tested", password = "tested"))
          user <- repo.updateByEmail(UserUpdateData(Some("updated@test.com"), None, None, Some("Updated test bio"), None), "test@test.com")
        } yield zio.test.assert(user) {
          Assertion.isSome {
            Assertion.hasField[UserData, Option[String]](
              "bio",
              _.bio, {
                Assertion.isSome {
                  Assertion.equalTo("Updated test bio")
                }
              }
            ) && Assertion.hasField[UserData, String](
              "email",
              _.email, {
                Assertion.equalTo("updated@test.com")
              }
            ) && Assertion.hasField[UserData, String](
              "username",
              _.username, {
                Assertion.equalTo("tested")
              }
            )
          }
        }
      }
    )
  ).provide(
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
