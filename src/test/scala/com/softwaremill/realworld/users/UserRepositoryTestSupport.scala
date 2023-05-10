package com.softwaremill.realworld.users

import com.softwaremill.diffx.{Diff, compare}
import com.softwaremill.realworld.users.api.{UserRegisterData, UserUpdateData}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.Assertion.*
import zio.test.{Assertion, TestAspect, TestRandom, TestResult, ZIOSpecDefault, assertTrue, assertZIO}
import zio.{RIO, Random, ZIO, ZLayer}

object UserRepositoryTestSupport:

  def callFindByEmail(email: String): ZIO[UsersRepository, Exception, Option[User]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserByEmail(email)
    } yield result
  }

  def callFindByUsername(username: String): ZIO[UsersRepository, Exception, Option[User]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserByUsername(username)
    } yield result
  }

  def callFindUserWithPasswordByEmail(email: String): ZIO[UsersRepository, Exception, Option[UserWithPassword]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserWithPasswordByEmail(email)
    } yield result
  }

  def callUserAdd(userRegisterData: UserRegisterData): ZIO[UsersRepository, Throwable, Unit] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.add(userRegisterData)
    } yield result
  }

  def callUpdateByEmail(userUpdateData: UserUpdateData, email: String): ZIO[UsersRepository, Throwable, Option[User]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.updateByEmail(userUpdateData, email)
    } yield result
  }

  def checkUserNotFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      result <- callFindByEmail(email)
    } yield zio.test.assert(result)(
      Assertion.equalTo(
        Option.empty
      )
    )
  }

  def checkUserNotFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      result <- callFindByUsername(username)
    } yield zio.test.assert(result)(
      Assertion.equalTo(
        Option.empty
      )
    )
  }

  def checkUserFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      userOpt <- callFindByEmail(email)
    } yield zio.test.assert(userOpt)(
      isSome(
        (hasField("email", _.email, equalTo("jake@example.com")): Assertion[User]) &&
          hasField("username", _.username, equalTo("jake")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    )
  }

  def checkUserFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      userOpt <- callFindByUsername(username)
    } yield zio.test.assert(userOpt)(
      isSome(
        (hasField("email", _.email, equalTo("jake@example.com")): Assertion[User]) &&
          hasField("username", _.username, equalTo("jake")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    )
  }

  def checkUserWithPasswordFound(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      userWithPasswordOpt <- callFindUserWithPasswordByEmail(email)
    } yield zio.test.assert(userWithPasswordOpt) {
      isSome(
        (hasField(
          "user",
          _.user,
          (hasField("email", _.email, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        ): Assertion[UserWithPassword]) &&
          hasField("hashedPassword", _.hashedPassword, isNonEmptyString)
      )
    }
  }

  def checkUserWithPasswordNotFound(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      result <- callFindUserWithPasswordByEmail(email)
    } yield zio.test.assert(result)(
      Assertion.equalTo(
        Option.empty
      )
    )
  }

  def checkUserAdd(userRegisterData: UserRegisterData): ZIO[UsersRepository, Throwable, TestResult] = {
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callFindByEmail(userRegisterData.email)
    } yield zio.test.assert(userOpt) {
      isSome(
        (hasField("email", _.email, equalTo("test@test.com")): Assertion[User]) &&
          hasField("username", _.username, equalTo("tested")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    }
  }

  def checkUpdateUserByBio(
      userRegisterData: UserRegisterData,
      userUpdateData: UserUpdateData
  ): ZIO[UsersRepository, Throwable, TestResult] = {
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callUpdateByEmail(userUpdateData, userRegisterData.email)
    } yield zio.test.assert(userOpt) {
      isSome {
        hasField[User, Option[String]](
          "bio",
          _.bio, {
            isSome {
              equalTo("Updated test bio")
            }
          }
        ) && hasField[User, String](
          "email",
          _.email, {
            equalTo("test@test.com")
          }
        ) && hasField[User, String](
          "username",
          _.username, {
            equalTo("tested")
          }
        )
      }
    }
  }

  def checkUpdateUserByBioAndEmail(
      userRegisterData: UserRegisterData,
      userUpdateData: UserUpdateData
  ): ZIO[UsersRepository, Throwable, TestResult] = {
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callUpdateByEmail(userUpdateData, userRegisterData.email)
    } yield zio.test.assert(userOpt) {
      isSome {
        hasField[User, Option[String]](
          "bio",
          _.bio, {
            isSome {
              equalTo("Updated test bio")
            }
          }
        ) && hasField[User, String](
          "email",
          _.email, {
            equalTo("updated@test.com")
          }
        ) && hasField[User, String](
          "username",
          _.username, {
            equalTo("tested")
          }
        )
      }
    }
  }
