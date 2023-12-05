package com.softwaremill.realworld.common

import sttp.model.{Header, StatusCode}
import sttp.monad.MonadError
import sttp.monad.syntax.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.interceptor.DecodeFailureContext
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DefaultDecodeFailureHandler}
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{EndpointIO, EndpointInput, headers, statusCode}

// Spec requires using invalid field name as a key in object returned in response.
// Tapir gives us only a message, thus custom handler.
// Problem mentioned in https://github.com/softwaremill/tapir/issues/2729
class CustomDecodeFailureHandler[F[_]](
    defaultHandler: DecodeFailureHandler[F],
    failureMessage: DecodeFailureContext => String,
    defaultRespond: DecodeFailureContext => Option[(StatusCode, List[Header])]
) extends DecodeFailureHandler[F]:

  override def apply(ctx: DecodeFailureContext)(using MonadError[F]): F[Option[ValuedEndpointOutput[_]]] = {
    ctx.failingInput match
      case EndpointInput.Query(name, _, _, _)    => getErrorResponseForField(name, ctx)
      case EndpointInput.PathCapture(name, _, _) => getErrorResponseForField(name.getOrElse("?"), ctx)
      case _: EndpointIO.Body[_, _]              => getErrorResponseForField("body", ctx)
      case _: EndpointIO.StreamBodyWrapper[_, _] => getErrorResponseForField("body", ctx)
      case _                                     => defaultHandler(ctx)
  }

  private def getErrorResponseForField(name: String, ctx: DecodeFailureContext)(using MonadError[F]): F[Option[ValuedEndpointOutput[_]]] = {
    val output = defaultRespond(ctx) match
      case Some((_, hs)) =>
        val failureMsg = failureMessage(ctx)
        Some(
          ValuedEndpointOutput(
            statusCode.and(headers).and(jsonBody[ValidationFailed]),
            (StatusCode.UnprocessableEntity, hs, ValidationFailed(Map(name -> List(failureMsg))))
          )
        )
      case None => None
    output.unit
  }

object CustomDecodeFailureHandler:

  def create[F[_]: MonadError](): DecodeFailureHandler[F] =
    new CustomDecodeFailureHandler[F](
      DefaultDecodeFailureHandler[F],
      FailureMessages.failureMessage,
      DefaultDecodeFailureHandler.respond
    )
