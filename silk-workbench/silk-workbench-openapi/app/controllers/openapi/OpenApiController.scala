package controllers.openapi

import akka.util.ByteString
import io.aurora.utils.play.swagger.SwaggerPlugin
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.{OpenAPI, PathItem, Paths}
import play.api.http.ContentTypes
import play.api.mvc._

import java.util.logging.{Level, Logger}
import javax.inject.Inject
import scala.jdk.CollectionConverters.mapAsScalaMapConverter
import scala.util.control.NonFatal

class OpenApiController @Inject()(cc: ControllerComponents,
                                  swaggerPlugin: SwaggerPlugin) extends AbstractController(cc) {

  private val AccessControlAllowOrigin: (String, String) = ("Access-Control-Allow-Origin", "*")

  def apiSpec: Action[AnyContent] = Action {
    val response = GenerateOpenApi(swaggerPlugin)
    Results
      .Ok(ByteString(response))
      .as(ContentTypes.JSON)
      .withHeaders(AccessControlAllowOrigin)
  }

  def ui: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.openapi.swaggerUi(routes.OpenApiController.apiSpec.absoluteURL()))
  }
}

/**
  * Generates OpenAPI documentation.
  */
object GenerateOpenApi {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  def apply(swaggerPlugin: SwaggerPlugin): String = {
    val host: String = swaggerPlugin.config.host
    val openApi = swaggerPlugin.apiListingCache.listing(host)
    sort(openApi)
    generateJson(openApi)
  }

  /**
    * At the moment, the sorting of the paths returned by the SwaggerPlugin is too random.
    * Thus, we use our own sorting.
    */
  def sort(openApi: OpenAPI): Unit = {
    val sortedPaths = new Paths()
    for((key, value) <- openApi.getPaths.asScala.toSeq.sortWith(comparePaths)) {
      sortedPaths.addPathItem(key, value)
    }
    openApi.setPaths(sortedPaths)
  }

  private def comparePaths(v1: (String, PathItem), v2: (String, PathItem)): Boolean = {
    val path1 = v1._1
    val path2 = v2._1
    val namespace1 = namespace(path1)
    val namespace2 = namespace(path2)
    if(namespace1.startsWith(namespace2) || namespace2.startsWith(namespace1)) {
      if(namespace1.length == namespace2.length) {
        path1.compareTo(path2) < 0
      } else {
        namespace1.length < namespace2.length
      }
    } else {
      path1.compareTo(path2) < 0
    }
  }

  @inline
  private def namespace(path: String): String = {
    val endIndex = path.lastIndexOf('/')
    if(endIndex != -1) {
      path.substring(0, endIndex)
    } else {
      path
    }
  }

  private def generateJson(openApi: OpenAPI): String = {
    try {
      Json.pretty().writeValueAsString(openApi)
    } catch {
      case NonFatal(t) =>
        log.log(Level.WARNING, "Issue with generating OpenAPI documentation", t)
        throw t
    }
  }

}