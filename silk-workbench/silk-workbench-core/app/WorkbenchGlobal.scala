
import java.util.logging.{Level, Logger}

import models.JsonError
import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.{Application, GlobalSettings}
import plugins.WorkbenchPlugins

import scala.concurrent.{ExecutionException, Future}

trait WorkbenchGlobal extends GlobalSettings with Rendering with AcceptExtractors {

  private val log = Logger.getLogger(getClass.getName)

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    Future.successful(BadRequest(JsonError(error)))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    //TODO  Should return HTML pages if the accept header includes HTML
    //    val res =
    //      render {
    //        case Accepts.Html() => InternalServerError(views.html.error(ex))
    //        case _ => InternalServerError(ex.getMessage)
    //      }
    //
    //    Future.successful(res)

    Future.successful(handleError(request.path, ex))
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
