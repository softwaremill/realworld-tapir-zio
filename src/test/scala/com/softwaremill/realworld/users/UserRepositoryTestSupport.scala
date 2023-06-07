package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.domain.Username
import com.softwaremill.realworld.users.api.{UserRegisterData, UserUpdateData}
import zio.ZIO
import zio.test.Assertion.*
import zio.test.{Assertion, TestResult, assertZIO}

import java.sql.SQLException

object UserRepositoryTestSupport:

  def callFindByEmail(email: String): ZIO[UsersRepository, Exception, Option[User]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserByEmail(email)
    } yield result

  def callFindById(id: Int): ZIO[UsersRepository, Exception, Option[User]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserById(id)
    } yield result

  def callFindByUsername(username: String): ZIO[UsersRepository, Exception, Option[User]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserByUsername(username)
    } yield result

  def callUserWithIdByUsername(username: Username): ZIO[UsersRepository, Exception, Option[(User, Int)]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserWithIdByUsername(username)
    } yield result

  def callFindUserIdByEmail(email: String): ZIO[UsersRepository, Exception, Option[Int]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserIdByEmail(email)
    } yield result
  }

  def callFindUserIdByUsername(username: String): ZIO[UsersRepository, Exception, Option[Int]] = {
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserIdByUsername(username)
    } yield result
  }

  def callFindUserWithPasswordByEmail(email: String): ZIO[UsersRepository, Exception, Option[UserWithPassword]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserWithPasswordByEmail(email)
    } yield result

  def callFindUserWithPasswordById(id: Int): ZIO[UsersRepository, Exception, Option[UserWithPassword]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.findUserWithPasswordById(id)
    } yield result

  def callUserAdd(userRegisterData: UserRegisterData): ZIO[UsersRepository, Throwable, Unit] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.add(userRegisterData)
    } yield result

  def callUpdateById(userUpdateData: UserUpdateData, userId: Int): ZIO[UsersRepository, Throwable, Option[User]] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.updateById(userUpdateData, userId)
    } yield result

  def callFollow(followedId: Int, followerId: Int): ZIO[UsersRepository, SQLException, Long] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.follow(followedId, followerId)
    } yield result

  def callUnfollow(followedId: Int, followerId: Int): ZIO[UsersRepository, SQLException, Long] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.unfollow(followedId, followerId)
    } yield result

  def callIsFollowing(followedId: Int, followerId: Int): ZIO[UsersRepository, SQLException, Boolean] =
    for {
      repo <- ZIO.service[UsersRepository]
      result <- repo.isFollowing(followedId, followerId)
    } yield result

  def checkUserNotFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    for {
      result <- callFindByEmail(email)
    } yield zio.test.assert(result)(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserNotFoundById(id: Int): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindById(id))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserNotFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindByUsername(username))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserWithIdNotFoundByUsername(username: Username): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callUserWithIdByUsername(username))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserIdNotFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserIdByEmail(email))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserIdNotFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserIdByUsername(username))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserWithPasswordNotFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserWithPasswordByEmail(email))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserWithPasswordNotFoundById(id: Int): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserWithPasswordById(id))(
      Assertion.equalTo(
        Option.empty
      )
    )

  def checkUserFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindByEmail(email))(
      isSome(
        (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
          hasField("username", _.username.value, equalTo("jake")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    )

  def checkUserFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindByUsername(username))(
      isSome(
        (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
          hasField("username", _.username.value, equalTo("jake")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    )

  def checkUserWithIdFoundByUsername(username: Username): ZIO[UsersRepository, Exception, TestResult] =
    for {
      userOpt <- callUserWithIdByUsername(username)
    } yield {
      zio.test.assert(userOpt.map(_._1))(
        isSome(
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        )
      ) && zio.test.assert(userOpt.map(_._2))(isSome(equalTo(1)))
    }

  def checkUserIdFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserIdByEmail(email))(isSome(equalTo(1)))

  def checkUserIdFoundByUsername(username: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserIdByUsername(username))(isSome(equalTo(1)))

  def checkUserFoundById(id: Int): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindById(id))(
      isSome(
        (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
          hasField("username", _.username.value, equalTo("jake")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    )

  def checkUserWithPasswordFoundByEmail(email: String): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserWithPasswordByEmail(email))(
      isSome(
        (hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        ): Assertion[UserWithPassword]) &&
          hasField("hashedPassword", _.hashedPassword, isNonEmptyString)
      )
    )

  def checkUserWithPasswordFoundById(userId: Int): ZIO[UsersRepository, Exception, TestResult] =
    assertZIO(callFindUserWithPasswordById(userId))(
      isSome(
        (hasField(
          "user",
          _.user,
          (hasField("email", _.email.value, equalTo("jake@example.com")): Assertion[User]) &&
            hasField("token", _.token, isNone) &&
            hasField("username", _.username.value, equalTo("jake")) &&
            hasField("bio", _.bio, isNone) &&
            hasField("image", _.image, isNone)
        ): Assertion[UserWithPassword]) &&
          hasField("hashedPassword", _.hashedPassword, isNonEmptyString)
      )
    )

  def checkUserAdd(userRegisterData: UserRegisterData): ZIO[UsersRepository, Throwable, TestResult] =
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callFindByEmail(userRegisterData.email)
    } yield zio.test.assert(userOpt) {
      isSome(
        (hasField("email", _.email.value, equalTo("test@test.com")): Assertion[User]) &&
          hasField("username", _.username.value, equalTo("tested")) &&
          hasField("bio", _.bio, isNone) &&
          hasField("image", _.image, isNone)
      )
    }

  def checkUpdateUserByBio(
      userRegisterData: UserRegisterData,
      userUpdateData: UserUpdateData
  ): ZIO[UsersRepository, Throwable, TestResult] =
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callUpdateById(userUpdateData, 1)
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
          _.email.value, {
            equalTo("test@test.com")
          }
        ) && hasField[User, String](
          "username",
          _.username.value, {
            equalTo("tested")
          }
        )
      }
    }

  def checkUpdateUserByBioAndEmail(
      userRegisterData: UserRegisterData,
      userUpdateData: UserUpdateData
  ): ZIO[UsersRepository, Throwable, TestResult] =
    for {
      _ <- callUserAdd(userRegisterData)
      userOpt <- callUpdateById(userUpdateData, 1)
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
          _.email.value, {
            equalTo("updated@test.com")
          }
        ) && hasField[User, String](
          "username",
          _.username.value, {
            equalTo("tested")
          }
        )
      }
    }

  def checkIsFollowing(
      followedId: Int,
      followerId: Int
  ): ZIO[UsersRepository, Throwable, TestResult] =
    assertZIO(callIsFollowing(followedId, followerId))(isTrue)

  def checkFollow(
      followedId: Int,
      followerId: Int
  ): ZIO[UsersRepository, Throwable, TestResult] =
    for {
      _ <- callFollow(followedId, followerId)
      isFollowing <- callIsFollowing(followedId, followerId)
    } yield zio.test.assert(isFollowing)(isTrue)

  def checkUnfollow(
      followedId: Int,
      followerId: Int
  ): ZIO[UsersRepository, Throwable, TestResult] =
    for {
      _ <- callUnfollow(followedId, followerId)
      isFollowing <- callIsFollowing(followedId, followerId)
    } yield zio.test.assert(isFollowing)(isFalse)
