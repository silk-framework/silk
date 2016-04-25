
import models.JsonError
import org.silkframework.workspace.{ProjectNotFoundException, TaskNotFoundException}
import play.api.PlayException.ExceptionSource
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, GlobalSettings}
import play.core.Router.Routes
import plugins.WorkbenchPlugins

import scala.concurrent.Future

trait WorkbenchGlobal extends GlobalSettings with Rendering with AcceptExtractors {

  /** The context path of this application, i.e. the URI prefix */
  protected var context: String = _

  private var pluginRoutes: Map[String, Routes] = _

  override def beforeStart(app: Application) {
    // Set context path
    context = app.configuration.getString("application.context").getOrElse("/")
    // Collect plugin routes
    pluginRoutes = WorkbenchPlugins().map(_.routes).reduce(_ ++ _)
    pluginRoutes = pluginRoutes.updated("core", core.Routes)
    for((prefix, routes) <- pluginRoutes if !(prefix == "core" && context != "/"))
      routes.setPrefix(context + prefix + "/")
  }

//  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
//    if(request.method == "OPTIONS") {
//      return super.onRouteRequest(request)
//    }
//
//    // Route to start page
//    if(request.path.stripSuffix("/") == context.stripSuffix("/")) {
//      return core.Routes.handlerFor(request.copy(path = context.stripSuffix("/") + "/core/start"))
//    }
//
//    // Route to registered modules
//    val prefix = request.path.stripPrefix(context).stripPrefix("/").takeWhile(_ != '/')
//    pluginRoutes.get(prefix) match {
//      case Some(routes) => routes.handlerFor(request)
//      case None => super.onRouteRequest(request)
//    }
//  }

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
