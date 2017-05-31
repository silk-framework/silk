package org.silkframework.workbench.utils

import java.util.logging.{Level, Logger}
import javax.inject.Provider

import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.http.{DefaultHttpErrorHandler, MimeTypes}
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, NotFound, Status}
import play.api.mvc.{AcceptExtractors, RequestHeader, Result, Results}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionException, Future}
import SilkErrorHandler.prefersHtml
import org.silkframework.serialization.json.JsonParseException

class SilkErrorHandler (env: Environment,
                        config: Configuration,
                        sourceMapper: OptionalSourceMapper,
                        router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with AcceptExtractors {

  private val log = Logger.getLogger(classOf[SilkErrorHandler].getName)

  /**
    * Invoked when a client error occurs, that is, an error in the 4xx series.
    *
    * @param request The request that caused the client error.
    * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
    * @param message The error message.
    */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if(prefersHtml(request)) {
      super.onClientError(request, statusCode, message)
    } else {
      val m = if(statusCode == 404 && message.isEmpty) "Not Found." else message
      Future {
        Status(statusCode)(JsonError(m))
      }
    }
  }

  /**
    * Invoked when a server error occurs.
    *
    * @param request The request that triggered the server error.
    * @param exception The server error.
    */
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    if(prefersHtml(request)) {
      super.onServerError(request, exception)
    } else {
      Future { handleError(request.path, exception) }
    }
  }

  /**
    * Invoked when a client makes a bad request.
    *
    * @param request The request that was bad.
    * @param message The error message.
    */
  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] =
    Future.successful(BadRequest(views.html.clientError(message)(request.session)))

  /**
    * Invoked when a client makes a request that was forbidden.
    *
    * @param request The forbidden request.
    * @param message The error message.
    */
  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Forbidden(views.html.clientError(message)(request.session)))
  }

  /**
    * Invoked when a client error occurs, that is, an error in the 4xx series, which is not handled by any of
    * the other methods.
    *
    * @param request The request that caused the client error.
    * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
    * @param message The error message.
    */
  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(Results.Status(statusCode)(views.html.clientError(message)(request.session)))
  }

  /**
    * Invoked in dev mode when a server error occurs.
    *
    * @param request The request that triggered the error.
    * @param exception The exception.
    */
  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(views.html.serverError(exception)(request.session)))
  }

  /**
    * Invoked in prod mode when a server error occurs.
    *
    * @param request The request that triggered the error.
    * @param exception The exception.
    */
  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(views.html.serverError(exception)(request.session)))
  }

  private def handleError(requestPath: String, ex: Throwable): Result = {
    ex match {
      case _: ExceptionSource if Option(ex.getCause).isDefined =>
        handleError(requestPath, ex.getCause)
      case _: ProjectNotFoundException | _: TaskNotFoundException =>
        NotFound(JsonError(ex))
      case executionException: ExecutionException =>
        Option(executionException.getCause) match {
          case Some(t) =>
            InternalServerError(JsonError(t))
          case None =>
            InternalServerError("Unknown error.")
        }
      case JsonParseException(msg, _) =>
        BadRequest(JsonError(msg))
      case _ =>
        log.log(Level.INFO, s"Error handling request to $requestPath", ex)
        InternalServerError(JsonError(ex))
    }
  }
}

object SilkErrorHandler {
  def prefersHtml(request: RequestHeader): Boolean = {
    val htmlIndex = request.acceptedTypes.indexWhere(_.accepts(MimeTypes.HTML))
    val jsonIndex = request.acceptedTypes.indexWhere(_.accepts(MimeTypes.JSON))

    htmlIndex > -1 && (jsonIndex < 0 || htmlIndex < jsonIndex)
  }
}