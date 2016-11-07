
import models.JsonError
import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.{Application, GlobalSettings}
import plugins.WorkbenchPlugins

import scala.concurrent.Future

trait WorkbenchGlobal extends GlobalSettings with Rendering with AcceptExtractors {

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
//TODO  Should return HTML pages if the accept header includes HTML
//    val res =
//      render {
//        case Accepts.Html() => InternalServerError(views.html.error(ex))
//        case _ => InternalServerError(ex.getMessage)
//      }
//
//    Future.successful(res)

    Future.successful(handleError(ex))
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
