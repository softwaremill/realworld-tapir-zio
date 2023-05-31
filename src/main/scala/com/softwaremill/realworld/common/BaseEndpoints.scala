package com.softwaremill.realworld.common

import com.softwaremill.realworld.articles.core.{ArticlesServerEndpoints, ArticlesService}
import com.softwaremill.realworld.auth.AuthService
import com.softwaremill.realworld.common.*
import com.softwaremill.realworld.common.BaseEndpoints.{UserWithEmailNotFoundMessage, defaultErrorOutputs}
import com.softwaremill.realworld.db.{Db, DbConfig}
import com.softwaremill.realworld.users.UsersRepository
import io.getquill.SnakeCase
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.ztapir.*
import sttp.tapir.{Codec, CodecFormat, Endpoint, EndpointIO, EndpointInput, EndpointOutput, Mapping, PublicEndpoint, Schema, TapirAuth, Validator}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}
import zio.{Cause, Exit, IO, Task, ZIO, ZLayer}
import sttp.model.HeaderNames
import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.Auth
import sttp.tapir.generic.auto.*

import scala.collection.immutable.ListMap
import sttp.tapir.Codec.*

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
case class UserSession(userId: Int)

class BaseEndpoints(authService: AuthService, usersRepository: UsersRepository):

  val secureEndpoint: ZPartialServerEndpoint[Any, String, UserSession, Unit, ErrorInfo, Unit, Any] = endpoint
    .errorOut(defaultErrorOutputs)
    .securityIn(http[String])
    .zServerSecurityLogic[Any, UserSession](handleAuth)

  val optionallySecureEndpoint: ZPartialServerEndpoint[Any, Option[String], Option[UserSession], Unit, ErrorInfo, Unit, Any] = endpoint
    .errorOut(defaultErrorOutputs)
    .securityIn(auth.http[Option[String]]("Token", WWWAuthenticateChallenge("Token")))
    .zServerSecurityLogic[Any, Option[UserSession]](handleAuthOpt)

  val publicEndpoint: PublicEndpoint[Unit, ErrorInfo, Unit, Any] = endpoint
    .errorOut(defaultErrorOutputs)


  /**
   * Mam problem z autoryzacją token albo bearer. Nie ma możliwości dokładnie przewidzieć jaka ta autoryzacja będzie,
   * czy to bedzie token czy bearer, musimy to podejmować na bieżąco. Zauwazylem, ze dostęp do oryginalnych tokenow
   * mamy w codecu, możemy dac metodę, że chcemy przefiltrować np. te tokeny, ale jak je wyciągnąć stamtąd?
   * - nie umiem je wyciagnąć z codeca, do zmiennych niemutowalnych
   * - gdy próbuje do zmiennych mutowalnych, zmieniam zmienna mutowalna w srodku funkcji, to ta globalna zmienna mutowalna nie
   *  zmienia swojej wartości
    */

  type StringListCodec[T] = Codec[List[String], T, CodecFormat.TextPlain]
  def http[T: StringListCodec]: EndpointInput.Auth[T, EndpointInput.AuthType.Http] = {
    val codec = implicitly[Codec[List[String], T, CodecFormat.TextPlain]]

    var authScheme: String = ""

    def filterHeaders[T: StringListCodec](headers: List[String]) =
      headers.filter(x => x.toLowerCase.startsWith("token") || x.toLowerCase.startsWith("bearer"))

    def stringPrefixWithSpace: Mapping[List[String], List[String]] =
      Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")

    val authCodec = Codec
      .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
      .map(x => {
        val filteredHeaders = filterHeaders(x)
        authScheme = filteredHeaders.head.split(" ").head.capitalize
        filteredHeaders
      })(identity)
      .map(stringPrefixWithSpace)
      .mapDecode(codec.decode)(codec.encode)
      .schema(codec.schema)

    EndpointInput.Auth(
      header[T](HeaderNames.Authorization)(authCodec),
      WWWAuthenticateChallenge(authScheme),
      EndpointInput.AuthType.Http(authScheme),
      EndpointInput.AuthInfo.Empty
    )
  }

  private def handleAuth(token: String): IO[ErrorInfo, UserSession] = {
    (for {
      userEmail <- authService.verifyJwt(token)
      userId <- userIdByEmail(userEmail)
    } yield UserSession(userId)).logError.mapError {
      case e: Exceptions.Unauthorized => Unauthorized(e.message)
      case e: Exceptions.NotFound     => NotFound(e.message)
      case _                          => InternalServerError()
    }
  }

  private def handleAuthOpt(tokenOpt: Option[String]): IO[ErrorInfo, Option[UserSession]] = {
    tokenOpt match
      case Some(token) => handleAuth(token).map(userSession => Option(userSession))
      case None        => ZIO.none
  }

  private def userIdByEmail(email: String): Task[Int] =
    usersRepository.findUserIdByEmail(email).someOrFail(Exceptions.NotFound(UserWithEmailNotFoundMessage(email)))

object BaseEndpoints:
  private val UserWithEmailNotFoundMessage: String => String = (email: String) => s"User with email $email doesn't exist"

  val live: ZLayer[AuthService with UsersRepository, Nothing, BaseEndpoints] = ZLayer.fromFunction(new BaseEndpoints(_, _))

  val defaultErrorOutputs: EndpointOutput.OneOf[ErrorInfo, ErrorInfo] = oneOf[ErrorInfo](
    oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
    oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[Forbidden])),
    oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
    oneOfVariant(statusCode(StatusCode.Conflict).and(jsonBody[Conflict])),
    oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized])),
    oneOfVariant(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ValidationFailed])),
    oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[InternalServerError]))
  )
