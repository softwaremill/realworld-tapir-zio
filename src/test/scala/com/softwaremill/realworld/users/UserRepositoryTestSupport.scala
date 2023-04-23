package com.softwaremill.realworld.users

import com.softwaremill.diffx.{Diff, compare}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.ziojson.*
import sttp.client3.{HttpError, Response, ResponseException, UriContext, basicRequest}
import sttp.tapir.EndpointOutput.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.{RIOMonadError, ZServerEndpoint}
import zio.test.Assertion.*
import zio.test.{Assertion, TestAspect, TestRandom, TestResult, ZIOSpecDefault, assertTrue, assertZIO}
import zio.{RIO, Random, ZIO, ZLayer}

object UserRepositoryTestSupport {

  def callFindByEmail(email: String): ZIO[UsersRepository, Exception, Option[UserRow]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findByEmail(email)
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

  def checkUserNotFound(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      result <- callFindByEmail(email)
    } yield zio.test.assert(result)(
      Assertion.equalTo(
        Option.empty
      )
    )
  }

  def checkUserFound(email: String): ZIO[UsersRepository, Exception, TestResult] = {
    for {
      userRowOpt <- callFindByEmail(email)
    } yield zio.test.assert(userRowOpt)(
      isSome(
        (hasField("userId", _.userId, equalTo(1)): Assertion[UserRow]) &&
          hasField("email", _.email, equalTo("admin@example.com")) &&
          hasField("username", _.username, equalTo("admin")) &&
          hasField("password", _.password, isNonEmptyString) &&
          hasField("bio", _.bio, equalTo(Some("I dont work"))) &&
          hasField("image", _.image, equalTo(Some("")))
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
          (hasField("email", _.email, equalTo("admin@example.com")): Assertion[UserData]) &&
            hasField("token", _.token, isSome) &&
            hasField("username", _.username, equalTo("admin")) &&
            hasField("bio", _.bio, equalTo(Some("I dont work"))) &&
            hasField("image", _.image, equalTo(Some("")))
        ): Assertion[UserWithPassword]) &&
          hasField("hashedPassword", _.hashedPassword, equalTo("password"))
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
      result <- callUserAdd(userRegisterData)
    } yield zio.test.assert(result)(isUnit) // TODO check DB?
  }
}
