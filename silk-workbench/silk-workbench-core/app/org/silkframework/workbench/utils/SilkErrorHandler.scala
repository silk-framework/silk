package org.silkframework.workbench.utils

import java.util.logging.{Level, Logger}
import javax.inject.Provider

import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.http.{DefaultHttpErrorHandler, MimeTypes}
import play.api.mvc.Results.{InternalServerError, NotFound, Status}
import play.api.mvc.{AcceptExtractors, RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionException, Future}
import SilkErrorHandler.prefersHtml

class SilkErrorHandler (env: Environment,
                        config: Configuration,
                        sourceMapper: OptionalSourceMapper,
                        router: Provider[Router]) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with AcceptExtractors {

  private val log = Logger.getLogger(classOf[SilkErrorHandler].getName)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if(prefersHtml(request)) {
      super.onClientError(request, statusCode, message)
    } else {
      Future {
        Status(statusCode)(JsonError(message))
      }
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    if(prefersHtml(request)) {
      super.onServerError(request, exception)
    } else {
      Future { handleError(request.path, exception) }
    }
  }

  override protected def onDevServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(views.html.error(exception)(request.session)))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(InternalServerError(views.html.error(exception)(request.session)))
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