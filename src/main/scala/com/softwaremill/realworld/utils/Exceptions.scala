package com.softwaremill.realworld.utils

object Exceptions:

  private val InvalidCredentialsMsg = "Invalid email or password!"

  case class BadRequest(message: String) extends RuntimeException(message)

  case class NotFound(message: String) extends RuntimeException(message)

  case class AlreadyInUse(message: String) extends RuntimeException(message)

  case class InvalidCredentials(message: String = InvalidCredentialsMsg) extends RuntimeException(message)
