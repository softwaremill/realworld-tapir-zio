package com.softwaremill.realworld.auth

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.{JWT, JWTVerifier}
import com.password4j.{Argon2Function, Password}
import com.softwaremill.realworld.auth.PasswordHashing.Argon2Config.*
import com.softwaremill.realworld.common.Exceptions
import com.softwaremill.realworld.common.Exceptions.{BadRequest, Unauthorized}
import com.softwaremill.realworld.users.UserData
import zio.{Clock, IO, RIO, ZIO, ZLayer}

import java.time.{Duration, Instant}
import java.util.UUID
import scala.util.{Failure, Success, Try}

// TODO change content below to AuthService

object PasswordHashing {

  private val Argon2: Argon2Function =
    Argon2Function.getInstance(MemoryInKib, NumberOfIterations, LevelOfParallelism, LengthOfTheFinalHash, Type, Version)

  object Argon2Config {
    val MemoryInKib = 12
    val NumberOfIterations = 20
    val LevelOfParallelism = 2
    val LengthOfTheFinalHash = 32
    val Type = com.password4j.types.Argon2.ID
    val Version = 19
  }

  def encryptPassword(password: String): IO[Exception, String] =
    Try(Password.hash(password).`with`(Argon2).getResult) match {
      case Success(hashedPassword) => ZIO.succeed(hashedPassword)
      case _                       => ZIO.fail(RuntimeException("Problem with password encryption."))
    }

  def verifyPassword(password: String, passwordHash: String): IO[Exception, Unit] = {
    if (Password.check(password, passwordHash) `with` PasswordHashing.Argon2) ZIO.succeed(())
    else ZIO.fail(Exceptions.Unauthorized())
  }
}

object JwtHandling {

  private val Issuer = "Softwaremill"
  private val ClaimName = "userEmail"
  private val YouShouldNotKeepSecretsHardcoded = "#>!IEd!G-L70@OTr$t8E[4.#[A;zo2@{"

  val algorithm: Algorithm = Algorithm.HMAC256(YouShouldNotKeepSecretsHardcoded)
  val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(Issuer)
      .build()

  def generateJwt(email: String): IO[Exception, String] = {
    val now: Instant = Instant.now()
    Try(
      JWT
        .create()
        .withIssuer(Issuer)
        .withClaim(ClaimName, email)
        .withIssuedAt(now)
        .withExpiresAt(now.plus(Duration.ofHours(1)))
        .withJWTId(UUID.randomUUID().toString)
        .sign(algorithm)
    ) match {
      case Success(createdJwt) => ZIO.succeed(createdJwt)
      case _                   => ZIO.fail(RuntimeException("Problem with JWT generation!"))
    }
  }

  def verifyJwt(jwtToken: String): IO[Exception, String] = {
    Try(verifier.verify(jwtToken)) match {
      case Success(decodedJwt) => ZIO.succeed(decodedJwt.getClaim(ClaimName).asString())
      case _                   => ZIO.fail(Unauthorized("Invalid token!"))
    }
  }
}
