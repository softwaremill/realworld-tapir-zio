package com.softwaremill.realworld.common

import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.{statusCode, stringBody}
import zio.Task

class DefectHandler extends ExceptionHandler[Task]:

  override def apply(ctx: ExceptionContext)(implicit monad: MonadError[Task]): Task[Option[ValuedEndpointOutput[_]]] =
    monad.unit(Some(ValuedEndpointOutput(statusCode.and(stringBody), (StatusCode.InternalServerError, "Internal server error"))))
