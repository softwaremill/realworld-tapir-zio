package com.softwaremill.realworld.auth

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.{JWT, JWTVerifier}
import com.password4j.{Argon2Function, Password}
import com.softwaremill.realworld.common.Exceptions.{BadRequest, Unauthorized}
import com.softwaremill.realworld.common.{AppConfig, Exceptions}
import zio.{Clock, IO, RIO, ZIO, ZLayer}

import java.time.{Duration, Instant}
import java.util.UUID
import scala.util.{Failure, Success, Try}

class AuthService(config: AppConfig):
  def encryptPassword(password: String): IO[Exception, String] = PasswordHashing.encryptPassword(password)
  def verifyPassword(password: String, passwordHash: String): IO[Exception, Unit] = PasswordHashing.verifyPassword(password, passwordHash)
  def generateJwt(email: String): IO[Exception, String] = JwtHandling.generateJwt(email)
  def verifyJwt(jwtToken: String): IO[Exception, String] = JwtHandling.verifyJwt(jwtToken)

  private object PasswordHashing:
    private final val MemoryInKib = 12
    private final val NumberOfIterations = 20
    private final val LevelOfParallelism = 2
    private final val LengthOfTheFinalHash = 32
    private final val Type = com.password4j.types.Argon2.ID
    private final val Version = 19

    private final val Argon2: Argon2Function =
      Argon2Function.getInstance(MemoryInKib, NumberOfIterations, LevelOfParallelism, LengthOfTheFinalHash, Type, Version)

    def encryptPassword(password: String): IO[Exception, String] =
      Try(Password.hash(password).`with`(Argon2).getResult) match {
        case Success(hashedPassword) => ZIO.succeed(hashedPassword)
        case _                       => ZIO.fail(RuntimeException("Problem with password encryption."))
      }

    def verifyPassword(password: String, passwordHash: String): IO[Exception, Unit] =
      if (Password.check(password, passwordHash) `with` PasswordHashing.Argon2) ZIO.succeed(())
      else ZIO.fail(Exceptions.Unauthorized())

  private object JwtHandling:
    private final val Issuer = "SoftwareMill"
    private final val ClaimName = "userEmail"

    private final val algorithm: Algorithm = Algorithm.HMAC256(config.system.jwtSecret)
    private final val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(Issuer).build()

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

    def verifyJwt(jwtToken: String): IO[Exception, String] =
      Try(verifier.verify(jwtToken)) match {
        case Success(decodedJwt) => ZIO.succeed(decodedJwt.getClaim(ClaimName).asString())
        case _                   => ZIO.fail(Unauthorized("Invalid token!"))
      }

object AuthService:
  val live: ZLayer[AppConfig, Nothing, AuthService] = ZLayer.fromFunction(new AuthService(_))
