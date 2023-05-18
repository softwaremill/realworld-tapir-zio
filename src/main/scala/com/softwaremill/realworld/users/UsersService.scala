package com.softwaremill.realworld.users

import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.Exceptions.{AlreadyInUse, BadRequest, NotFound, Unauthorized}
import com.softwaremill.realworld.common.{Exceptions, Pagination, UserSession}
import com.softwaremill.realworld.users.UsersService.*
import com.softwaremill.realworld.users.api.*
import org.apache.commons.validator.routines.EmailValidator
import zio.{Console, IO, Task, ZIO, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource

class UsersService(authService: AuthService, usersRepository: UsersRepository):

  def get(userId: Int): IO[Exception, User] = usersRepository
    .findUserById(userId)
    .someOrFail(NotFound(UserWithIdNotFoundMessage(userId)))

  def register(user: UserRegisterData): IO[Throwable, UserResponse] = {
    val emailClean = user.email.toLowerCase.trim()
    val usernameClean = user.username.trim()
    val passwordClean = user.password.trim()

    def checkUserDoesNotExist(email: String, username: String): Task[Unit] =
      for {
        _ <- checkUserDoesNotExistByEmail(email)
        _ <- checkUserDoesNotExistByUsername(username)
      } yield ()

    for {
      _ <- validateEmail(emailClean)
      _ <- checkUserDoesNotExist(emailClean, usernameClean)
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
      _ <- validateEmail(emailClean)
      maybeUser <- usersRepository.findUserWithPasswordByEmail(emailClean)
      userWithPassword <- ZIO.fromOption(maybeUser).mapError(_ => Unauthorized())
      _ <- authService.verifyPassword(passwordClean, userWithPassword.hashedPassword)
      jwt <- authService.generateJwt(emailClean)
    } yield userWithPassword.user.copy(token = Some(jwt))
  }

  def update(updateData: UserUpdateData, userId: Int): IO[Throwable, User] =
    val emailCleanOpt = updateData.email.map(email => email.toLowerCase.trim())
    val usernameCleanOpt = updateData.username.map(username => username.trim())

    for {
      _ <- ZIO.foreach(emailCleanOpt)(validateEmail)
      _ <- ZIO.foreach(emailCleanOpt)(emailClean => checkUserDoesNotExistByEmail(emailClean, userId))
      _ <- ZIO.foreach(usernameCleanOpt)(usernameClean => checkUserDoesNotExistByUsername(usernameClean, userId))
      oldUser <- usersRepository
        .findUserWithPasswordById(userId)
        .someOrFail(NotFound(UserWithIdNotFoundMessage(userId)))
      password <- updateData.password
        .map(newPassword => authService.encryptPassword(newPassword))
        .getOrElse(ZIO.succeed(oldUser.hashedPassword))
      updatedUser <- usersRepository
        .updateById(
          updateData.update(oldUser.copy(hashedPassword = password)),
          userId
        )
        .someOrFail(NotFound(UserWithIdNotFoundMessage(userId)))
    } yield updatedUser

  def getProfile(username: String, followerId: Int): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (followed, followedId) = userWithIdTuple
    profile <- getProfileData(followed, followedId, Some(followerId))
  } yield ProfileResponse(profile)

  def follow(username: String, followerId: Int): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (followed, followedId) = userWithIdTuple
    _ <- ZIO.fail(BadRequest(CannotFollowYourselfMessage)).when(followedId == followerId)
    _ <- usersRepository.follow(followedId, followerId)
    profile <- getProfileData(followed, followedId, Some(followerId))
  } yield ProfileResponse(profile)

  def unfollow(username: String, followerId: Int): Task[ProfileResponse] = for {
    userWithIdTuple <- getUserWithIdByUsername(username)
    (followed, followedId) = userWithIdTuple
    _ <- usersRepository.unfollow(followedId, followerId)
    profile <- getProfileData(followed, followedId, Some(followerId))
  } yield ProfileResponse(profile)

  private def userWithToken(email: String, username: String, jwt: String): User =
    User(
      email,
      Some(jwt),
      username,
      Option.empty[String],
      Option.empty[String]
    )

  private def checkUserDoesNotExistByEmail(email: String): Task[Unit] =
    for {
      maybeUserByEmail <- usersRepository.findUserByEmail(email)
      _ <- ZIO.fail(AlreadyInUse(UserWithEmailAlreadyInUseMessage(email))).when(maybeUserByEmail.isDefined)
    } yield ()

  private def checkUserDoesNotExistByUsername(username: String): Task[Unit] =
    for {
      maybeUserByUsername <- usersRepository.findUserByUsername(username)
      _ <- ZIO.fail(AlreadyInUse(UserWithUsernameAlreadyInUseMessage(username))).when(maybeUserByUsername.isDefined)
    } yield ()

  private def checkUserDoesNotExistByEmail(email: String, userIdFromSession: Int): Task[Unit] =
    for {
      maybeUserByEmail <- usersRepository.findUserByEmail(email)
      maybeUserFromSession <- usersRepository.findUserById(userIdFromSession)
      _ <- ZIO
        .fail(AlreadyInUse(UserWithEmailAlreadyInUseMessage(email)))
        .when(
          maybeUserByEmail.isDefined && maybeUserByEmail
            .flatMap(userByUsername => maybeUserFromSession.map(userBySession => userByUsername.email != userBySession.email))
            .getOrElse(true)
        )
    } yield ()

  private def checkUserDoesNotExistByUsername(username: String, userIdFromSession: Int): Task[Unit] =
    for {
      maybeUserByUsername <- usersRepository.findUserByUsername(username)
      maybeUserFromSession <- usersRepository.findUserById(userIdFromSession)
      _ <- ZIO
        .fail(AlreadyInUse(UserWithUsernameAlreadyInUseMessage(username)))
        .when(
          maybeUserByUsername.isDefined && maybeUserByUsername
            .flatMap(userByUsername => maybeUserFromSession.map(userBySession => userByUsername.username != userBySession.username))
            .getOrElse(true)
        )
    } yield ()

  private def validateEmail(email: String): Task[Unit] = {
    val emailValidator = EmailValidator.getInstance()

    if (!emailValidator.isValid(email)) ZIO.fail(BadRequest(InvalidEmailMessage(email))) else ZIO.unit
  }
  private def getUserWithIdByUsername(username: String): Task[(User, Int)] = usersRepository
    .findUserWithIdByUsername(username)
    .someOrFail(NotFound(UserWithUsernameNotFoundMessage(username)))

  private def getProfileData(user: User, userId: Int, asSeenByUserWithIdOpt: Option[Int]): Task[Profile] =
    asSeenByUserWithIdOpt match
      case Some(asSeenByUserWithId) =>
        usersRepository.isFollowing(userId, asSeenByUserWithId).map(Profile(user.username, user.bio, user.image, _))
      case None => ZIO.succeed(Profile(user.username, user.bio, user.image, false))

object UsersService:
  private val InvalidEmailMessage: String => String = (email: String) => s"Email $email is not valid"
  private val UserWithIdNotFoundMessage: Int => String = (id: Int) => s"User with id $id doesn't exist"
  private val UserWithUsernameNotFoundMessage: String => String = (username: String) => s"User with username $username doesn't exist"
  private val UserWithEmailAlreadyInUseMessage: String => String = (email: String) => s"User with email $email already in use"
  private val UserWithUsernameAlreadyInUseMessage: String => String = (username: String) => s"User with username $username already in use"
  private val CannotFollowYourselfMessage: String = "You can't follow yourself"

  val live: ZLayer[AuthService with UsersRepository, Nothing, UsersService] = ZLayer.fromFunction(UsersService(_, _))
