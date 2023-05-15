package com.softwaremill.realworld.users

import com.softwaremill.realworld.db.{Db, DbConfig, DbMigrator}
import com.softwaremill.realworld.users.UserDbTestSupport.*
import com.softwaremill.realworld.users.UserRepositoryTestSupport.*
import com.softwaremill.realworld.users.api.{UserRegisterData, UserUpdateData}
import com.softwaremill.realworld.utils.TestUtils.*
import zio.test.ZIOSpecDefault

object UsersRepositorySpec extends ZIOSpecDefault:
  override def spec = suite("user repository tests")(
    suite("find user by email")(
      test("user not found") {
        for {
          _ <- prepareOneUser
          result <- checkUserNotFoundByEmail("notExisting@example.com")
        } yield result
      },
      test("user found") {
        for {
          _ <- prepareOneUser
          result <- checkUserFoundByEmail("jake@example.com")
        } yield result
      }
    ),
    suite("find user by id")(
      test("user not found") {
        for {
          _ <- prepareOneUser
          result <- checkUserNotFoundById(0)
        } yield result
      },
      test("user found") {
        for {
          _ <- prepareOneUser
          result <- checkUserFoundById(1)
        } yield result
      }
    ),
    suite("find user by username")(
      test("user not found") {
        for {
          _ <- prepareOneUser
          result <- checkUserNotFoundByUsername("notExisting")
        } yield result
      },
      test("user found") {
        for {
          _ <- prepareOneUser
          result <- checkUserFoundByUsername("jake")
        } yield result
      }
    ),
    suite("find user with password by email")(
      test("user with password not found") {
        for {
          _ <- prepareOneUser
          result <- checkUserWithPasswordNotFoundByEmail("notExisting@example.com")
        } yield result
      },
      test("user with password found") {
        for {
          _ <- prepareOneUser
          result <- checkUserWithPasswordFoundByEmail("jake@example.com")
        } yield result
      }
    ),
    suite("find user with password by id")(
      test("user with password not found") {
        for {
          _ <- prepareOneUser
          result <- checkUserWithPasswordNotFoundById(0)
        } yield result
      },
      test("user with password found") {
        for {
          _ <- prepareOneUser
          result <- checkUserWithPasswordFoundById(1)
        } yield result
      }
    ),
    suite("add user")(
      test("user added") {
        for {
          _ <- prepareOneUser
          result <- checkUserAdd(UserRegisterData(email = "test@test.com", username = "tested", password = "tested"))
        } yield result
      }
    ),
    suite("update user")(
      test("update user bio") {
        for {
          result <- checkUpdateUserByBio(
            userRegisterData = UserRegisterData(email = "test@test.com", username = "tested", password = "tested"),
            userUpdateData = UserUpdateData(None, None, None, Some("Updated test bio"), None)
          )
        } yield result
      },
      test("update user bio and email") {
        for {
          result <- checkUpdateUserByBioAndEmail(
            userRegisterData = UserRegisterData(email = "test@test.com", username = "tested", password = "tested"),
            userUpdateData = UserUpdateData(Some("updated@test.com"), None, None, Some("Updated test bio"), None)
          )
        } yield result
      }
    )
  ).provide(
    UsersRepository.live,
    testDbLayerWithEmptyDb
  )
