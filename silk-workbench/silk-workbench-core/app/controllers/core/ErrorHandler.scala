package controllers.core

import models.JsonError
import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ErrorHandler extends HttpErrorHandler {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future { NotFound(JsonError(message)) }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future { handleError(exception) }
  }

  private def handleError(ex: Throwable): Result = {
    ex match {
      case _: ExceptionSource if ex.getCause != null =>
        handleError(ex.getCause)
      case _: ProjectNotFoundException | _: TaskNotFoundException =>
        NotFound(JsonError(ex))
      case _ =>
        InternalServerError(JsonError(ex))
    }
  }
}
