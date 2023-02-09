package com.softwaremill.realworld

import com.softwaremill.realworld.utils.ValidationFailed
import sttp.model.{Header, StatusCode}
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.interceptor.DecodeFailureContext
import sttp.tapir.server.interceptor.decodefailure.{DecodeFailureHandler, DefaultDecodeFailureHandler}
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.FailureMessages
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{EndpointIO, EndpointInput, headers, statusCode, stringBody}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

class CustomDecodeFailureHandler(
    defaultHandler: DecodeFailureHandler,
    failureMessage: DecodeFailureContext => String,
    defaultRespond: DecodeFailureContext => Option[(StatusCode, List[Header])]
) extends DecodeFailureHandler:

  override def apply(ctx: DecodeFailureContext): Option[ValuedEndpointOutput[_]] = {
    ctx.failingInput match
      case EndpointInput.Query(name, _, _, _)    => getOutput(name, ctx)
      case EndpointInput.PathCapture(name, _, _) => getOutput(name.getOrElse("?"), ctx)
      case _: EndpointIO.Body[_, _]              => getOutput("body", ctx)
      case _: EndpointIO.StreamBodyWrapper[_, _] => getOutput("body", ctx)
      case _                                     => defaultHandler(ctx)
  }
  private def getOutput(name: String, ctx: DecodeFailureContext): Option[ValuedEndpointOutput[_]] = {
    defaultRespond(ctx) match
      case Some((_, hs)) =>
        val failureMsg = failureMessage(ctx)
        Some(
          ValuedEndpointOutput(
            statusCode.and(headers).and(jsonBody[ValidationFailed]),
            (StatusCode.UnprocessableEntity, hs, ValidationFailed(Map(name -> List(failureMsg))))
          )
        )
      case None => None
  }

object CustomDecodeFailureHandler:

  def create(): DecodeFailureHandler =
    new CustomDecodeFailureHandler(
      DefaultDecodeFailureHandler.default,
      FailureMessages.failureMessage,
      DefaultDecodeFailureHandler.respond(_, false, true)
    )
