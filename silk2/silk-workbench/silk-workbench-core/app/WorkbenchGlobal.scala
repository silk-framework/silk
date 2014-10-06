
import play.api.mvc.Results._
import play.api.mvc.{Handler, RequestHeader}
import play.api.{Logger, Application, GlobalSettings}
import play.core.Router.Routes
import plugins.WorkbenchPlugins
import scala.concurrent.Future

trait WorkbenchGlobal extends GlobalSettings {

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
    if(request.path == context)
      return core.Routes.handlerFor(request.copy(path = context + "core/"))

    // Route to registered modules
    val prefix = request.path.stripPrefix(context).takeWhile(_ != '/')
    pluginRoutes.get(prefix) match {
      case Some(routes) => routes.handlerFor(request)
      case None => super.onRouteRequest(request)
    }
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(views.html.error(ex)))
  }

}
