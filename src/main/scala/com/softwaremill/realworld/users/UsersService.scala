package com.softwaremill.realworld.users

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.Exceptions.{AlreadyInUse, BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination}
import com.softwaremill.realworld.users.UsersService.*
import com.softwaremill.realworld.users.api.*
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource

class UsersService(authService: AuthService, usersRepository: UsersRepository):

  def get(email: String): IO[Exception, User] = usersRepository
    .findUserByEmail(email)
    .someOrFail(NotFound(UserWithEmailNotFoundMessage(email)))

  // TODO username should also be checked (in database is unique)
  def register(user: UserRegisterData): IO[Throwable, UserResponse] = {
    val emailClean = user.email.toLowerCase.trim()
    val usernameClean = user.username.trim()
    val passwordClean = user.password.trim()

    def checkUserDoesNotExist(email: String): IO[Exception, Unit] =
      for {
        maybeUser <- usersRepository.findUserByEmail(email.toLowerCase)
        _ <- ZIO.fail(AlreadyInUse(UserAlreadyInUseMessage(email))).when(maybeUser.isDefined)
      } yield ()

    for {
      _ <- checkUserDoesNotExist(emailClean)
      user <- {
        for {
          hashedPassword <- authService.encryptPassword(passwordClean)
          jwt <- authService.generateJwt(emailClean)
          _ <- usersRepository.add(UserRegisterData(emailClean, usernameClean, hashedPassword))
        } yield UserResponse(userWithToken(emailClean, usernameClean, jwt))
      }
    } yield user
  }

  def login(user: UserLoginData): IO[Throwable, User] = {
    val emailClean = user.email.toLowerCase.trim()
    val passwordClean = user.password.trim()

    for {
      maybeUser <- usersRepository.findUserWithPasswordByEmail(emailClean)
      userWithPassword <- ZIO.fromOption(maybeUser).mapError(_ => Unauthorized())
      _ <- authService.verifyPassword(passwordClean, userWithPassword.hashedPassword)
      jwt <- authService.generateJwt(emailClean)
    } yield userWithPassword.user.copy(token = Some(jwt))
  }

  def update(updateData: UserUpdateData, email: String): IO[Throwable, User] =
    for {
      oldUser <- usersRepository
        .findUserWithPasswordByEmail(email)
        .someOrFail(NotFound(UserWithEmailNotFoundMessage(email)))
      password <- updateData.password
        .map(newPassword => authService.encryptPassword(newPassword))
        .getOrElse(ZIO.succeed(oldUser.hashedPassword))
      updatedUser <- usersRepository
        .updateByEmail(
          updateData.update(oldUser.copy(hashedPassword = password)),
          email
        )
        .someOrFail(NotFound(UserWithEmailNotFoundMessage(email)))
    } yield updatedUser

  def getProfile(username: String, viewerEmail: String): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (user, userId) = userWithIdTuple
    viewerId <- getFollowerIdByEmail(viewerEmail)
    profile <- getProfileData(user, userId, Some(viewerId))
  } yield ProfileResponse(profile)

  def follow(username: String, followerEmail: String): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (user, userId) = userWithIdTuple
    followerId <- getFollowerIdByEmail(followerEmail)
    _ <- ZIO.fail(BadRequest(CannotFollowYourselfMessage)).when(userId == followerId)
    _ <- usersRepository.follow(userId, followerId)
    profile <- getProfileData(user, userId, Some(followerId))
  } yield ProfileResponse(profile)

  def unfollow(username: String, followerEmail: String): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (user, userId) = userWithIdTuple
    followerId <- getFollowerIdByEmail(followerEmail)
    _ <- usersRepository.unfollow(userId, followerId)
    profile <- getProfileData(user, userId, Some(followerId))
  } yield ProfileResponse(profile)

  private def userWithToken(email: String, username: String, jwt: String): User = {
    User(
      email,
      Some(jwt),
      username,
      Option.empty[String],
      Option.empty[String]
    )
  }

  private def getUserWithIdByUsername(username: String): Task[(User, Int)] = usersRepository
    .findUserWithIdByUsername(username)
    .someOrFail(NotFound(UserWithUsernameNotFoundMessage(username)))

  private def getFollowerIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(NotFound(UserWithEmailNotFoundMessage(email)))

  private def getProfileData(user: User, userId: Int, asSeenByUserWithIdOpt: Option[Int]): Task[Profile] =
    asSeenByUserWithIdOpt match
      case Some(asSeenByUserWithId) =>
        usersRepository.isFollowing(userId, asSeenByUserWithId).map(Profile(user.username, user.bio, user.image, _))
      case None => ZIO.succeed(Profile(user.username, user.bio, user.image, false))

object UsersService:
  private val UserWithEmailNotFoundMessage: String => String = (email: String) => s"User with email $email doesn't exist"
  private val UserWithUsernameNotFoundMessage: String => String = (username: String) => s"User with username $username doesn't exist"
  private val UserAlreadyInUseMessage: String => String = (email: String) => s"User with email $email already in use"
  private val CannotFollowYourselfMessage: String = "You can't follow yourself"

  val live: ZLayer[AuthService with UsersRepository, Nothing, UsersService] = ZLayer.fromFunction(UsersService(_, _))
