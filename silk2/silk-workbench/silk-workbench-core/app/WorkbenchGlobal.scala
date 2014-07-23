
import play.api.mvc.Results._
import play.api.mvc.{Handler, RequestHeader}
import play.api.{Application, GlobalSettings}
import play.core.Router.Routes
import plugins.WorkbenchPlugins
import scala.concurrent.Future

trait WorkbenchGlobal extends GlobalSettings {

  private var pluginRoutes = Map[String, Routes]()

  override def beforeStart(app: Application) {
    // Collect plugin routes
    pluginRoutes = WorkbenchPlugins().map(_.routes).reduce(_ ++ _)
    for((prefix, routes) <- pluginRoutes)
      routes.setPrefix("/" + prefix + "/")
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    // Route to start page
    if(request.path == "/")
      return core.Routes.handlerFor(request.copy(path = "/core"))

    // Route to registered modules
    val prefix = request.path.stripPrefix("/").takeWhile(_ != '/')
    pluginRoutes.get(prefix) match {
      case Some(routes) => routes.handlerFor(request)
      case None => super.onRouteRequest(request)
    }
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(ex.getMessage))
  }

}
