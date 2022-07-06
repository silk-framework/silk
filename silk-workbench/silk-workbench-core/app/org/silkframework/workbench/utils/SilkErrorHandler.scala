package org.silkframework.workbench.utils

import config.WorkbenchConfig.WorkspaceReact
import org.silkframework.runtime.validation._
import org.silkframework.workbench.utils.SilkErrorHandler.prefersHtml
import play.api.PlayException.ExceptionSource
import play.api._
import play.api.http.Status._
import play.api.http.{DefaultHttpErrorHandler, MimeTypes}
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, NotFound, ServiceUnavailable}
import play.api.mvc.{AcceptExtractors, RequestHeader, Result, Results}
import play.api.routing.Router

import java.util.logging.{Level, Logger}
import javax.inject.Provider
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionException, Future}

class SilkErrorHandler (env: Environment,
                        config: Configuration,
                        sourceMapper: OptionalSourceMapper,
                        router: Provider[Router],
                        workspaceReact: WorkspaceReact) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with AcceptExtractors {

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
      Future {
        if (statusCode == 404 && message.isEmpty) {
          ErrorResult(statusCode, title = "Not Found", detail = "Not Found")
        } else {
          ErrorResult(statusCode, title = "Client error", detail = message)
        }
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
    Future.successful(BadRequest(views.html.clientError(message)(request, workspaceReact)))

  /**
    * Invoked when a client makes a request that was forbidden.
    *
    * @param request The forbidden request.
    * @param message The error message.
    */
  override protected def onForbidden(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Forbidden(views.html.clientError(message)(request, workspaceReact)))
  }

  /**
    * Invoked when a handler or resource is not found.
    *
    * @param request The request that no handler was found to handle.
    * @param message A message.
    */
  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(NotFound(views.html.defaultpages.devNotFound(request.method, request.uri, Some(router.get))(request)))
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
    Future.successful(Results.Status(statusCode)(views.html.clientError(message)(request, workspaceReact)))
  }

  /**
    * Invoked in dev mode when a server error occurs.
    *
    * @param request The request that triggered the error.
    * @param exception The exception.
    */
  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    errorPage(request, exception)
  }

  // Return the error page
  private def errorPage(request: RequestHeader, exception: UsefulException) = {
    exception.cause match {
      case _: ServiceUnavailableException =>
        Future.successful(ServiceUnavailable(
          views.html.serverError(
            exception, "info",
            showExceptionId = false,
            showDetails = false,
            title = Some("Service temporary unavailable"),
            details = Option(exception.cause.getMessage)
          )(request.session, workspaceReact)))
      case _ =>
        Future.successful(InternalServerError(views.html.serverError(exception, "danger")(request.session, workspaceReact)))
    }
  }

  /**
    * Invoked in prod mode when a server error occurs.
    *
    * @param request The request that triggered the error.
    * @param exception The exception.
    */
  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    errorPage(request, exception)
  }

  private def handleError(requestPath: String, ex: Throwable): Result = {
    ex match {
      case ex: NotAuthorizedException =>
        ErrorResult(UNAUTHORIZED, "Unauthorized", ex.msg)
      case ex: ForbiddenException =>
        ErrorResult(FORBIDDEN, "Forbidden", ex.msg)
      case ex: BadUserInputException =>
        ErrorResult(BAD_REQUEST, "Bad request", ex.msg)
      case _: ExceptionSource if Option(ex.getCause).isDefined =>
        handleError(requestPath, ex.getCause)
      case executionException: ExecutionException =>
        Option(executionException.getCause) match {
          case Some(t) =>
            ErrorResult.serverError(INTERNAL_SERVER_ERROR, t)
          case None =>
            ErrorResult.serverError(INTERNAL_SERVER_ERROR, executionException)
        }
      case requestEx: RequestException =>
        ErrorResult(requestEx)
      case _ =>
        log.log(Level.INFO, s"Error handling request to $requestPath", ex)
        ErrorResult.serverError(INTERNAL_SERVER_ERROR, ex)
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