package com.softwaremill.realworld.users

import com.softwaremill.realworld.common.Exceptions
import com.softwaremill.realworld.users.api.{UserRegisterData, UserUpdateData}
import io.getquill.*
import io.getquill.jdbczio.*
import org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE
import org.sqlite.SQLiteException
import zio.{IO, RIO, Task, ZIO, ZLayer}

import java.sql.SQLException
import scala.util.chaining.*

case class FollowerRow(userId: Int, followerId: Int)

case class UserRow(
    userId: Int,
    email: String,
    username: String,
    password: String,
    bio: Option[String],
    image: Option[String]
)

class UsersRepository(quill: Quill.Sqlite[SnakeCase]):
  import quill.*

  private inline def queryFollower = quote(querySchema[FollowerRow](entity = "followers"))
  private inline def queryUser = quote(querySchema[UserRow](entity = "users"))

  def findUserByEmail(email: String): IO[Exception, Option[User]] =
    run(queryUser.filter(ur => ur.email == lift(email)))
      .map(_.headOption)
      .map(_.map(user))

  def findUserById(userId: Int): IO[Exception, Option[User]] =
    run(queryUser.filter(ur => ur.userId == lift(userId)))
      .map(_.headOption)
      .map(_.map(user))

  def findUserByUsername(username: String): IO[Exception, Option[User]] =
    run(queryUser.filter(ur => ur.username == lift(username)))
      .map(_.headOption)
      .map(_.map(user))

  def findUserWithIdByUsername(username: String): IO[Exception, Option[(User, Int)]] =
    run(queryUser.filter(ur => ur.username == lift(username)))
      .map(_.headOption)
      .map(_.map(userRow => (user(userRow), userRow.userId)))

  def findUserIdByEmail(email: String): IO[Exception, Option[Int]] =
    run(queryUser.filter(ur => ur.email == lift(email)))
      .map(_.headOption)
      .map(_.map(_.userId))

  def findUserIdByUsername(username: String): IO[Exception, Option[Int]] =
    run(queryUser.filter(ur => ur.username == lift(username)))
      .map(_.headOption)
      .map(_.map(_.userId))

  def findUserWithPasswordByEmail(email: String): IO[Exception, Option[UserWithPassword]] =
    run(queryUser.filter(ur => ur.email == lift(email)))
      .map(_.headOption)
      .map(_.map(userWithPassword))

  def findUserWithPasswordById(id: Int): IO[Exception, Option[UserWithPassword]] =
    run(queryUser.filter(ur => ur.userId == lift(id)))
      .map(_.headOption)
      .map(_.map(userWithPassword))

  def add(user: UserRegisterData): Task[Unit] = run(
    queryUser
      .insert(
        _.email -> lift(user.email),
        _.username -> lift(user.username),
        _.password -> lift(user.password)
      )
  ).unit
    .pipe(mapUniqueConstraintViolationError)

  def updateById(updateData: UserUpdateData, userId: Int): IO[Throwable, Option[User]] = {
    val update = queryUser.dynamic
      .filter(_.userId == lift(userId))
      .update(
        setOpt[UserRow, String](_.email, updateData.email),
        setOpt[UserRow, String](_.username, updateData.username),
        setOpt[UserRow, String](_.password, updateData.password),
        setOpt[UserRow, String](_.bio.orNull, updateData.bio),
        setOpt[UserRow, String](_.image.orNull, updateData.image)
      )

    val read = quote(
      queryUser
        .filter(_.userId == lift(userId))
        .value
    )

    transaction {
      run(update)
        .flatMap(_ => run(read))
        .map(_.map(user))
    }
  }

  def follow(followedId: Int, followerId: Int): IO[SQLException, Long] = run {
    queryFollower.insert(_.userId -> lift(followedId), _.followerId -> lift(followerId)).onConflictIgnore
  }

  def unfollow(followedId: Int, followerId: Int): IO[SQLException, Long] = run {
    queryFollower.filter(f => (f.userId == lift(followedId)) && (f.followerId == lift(followerId))).delete
  }

  def isFollowing(followedId: Int, followerId: Int): IO[SQLException, Boolean] = run {
    queryFollower.filter(_.userId == lift(followedId)).filter(_.followerId == lift(followerId)).map(_ => 1).nonEmpty
  }

  private def user(userRow: UserRow): User =
    User(
      email = userRow.email,
      token = None,
      username = userRow.username,
      bio = userRow.bio,
      image = userRow.image
    )

  private def userWithPassword(userRow: UserRow): UserWithPassword =
    UserWithPassword(
      user = user(userRow),
      hashedPassword = userRow.password
    )

  private def mapUniqueConstraintViolationError[R, A](task: RIO[R, A]): RIO[R, A] = task.mapError {
    case e: SQLiteException if e.getResultCode == SQLITE_CONSTRAINT_UNIQUE =>
      Exceptions.AlreadyInUse("Given user data is already in use")
    case e => e
  }

object UsersRepository:
  val live: ZLayer[Quill.Sqlite[SnakeCase], Nothing, UsersRepository] =
    ZLayer.fromFunction(new UsersRepository(_))
