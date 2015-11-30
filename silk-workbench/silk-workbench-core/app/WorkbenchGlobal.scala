
import play.api.PlayException.ExceptionSource
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Logger, Application, GlobalSettings}
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

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    // Route to start page
    if(request.path.stripSuffix("/") == context.stripSuffix("/")) {
      return core.Routes.handlerFor(request.copy(path = context.stripSuffix("/") + "/core/start"))
    }

    // Route to registered modules
    val prefix = request.path.stripPrefix(context).stripPrefix("/").takeWhile(_ != '/')
    pluginRoutes.get(prefix) match {
      case Some(routes) => routes.handlerFor(request)
      case None => super.onRouteRequest(request)
    }
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
    if(ex.isInstanceOf[ExceptionSource] && ex.getCause != null) {
      Future.successful(InternalServerError(ex.getCause.getMessage))
    } else {
      Future.successful(InternalServerError(ex.getMessage))
    }
  }

}
