package controllers.openapi

import akka.util.ByteString
import io.aurora.utils.play.swagger.SwaggerPlugin
import org.silkframework.openapi.OpenApiGenerator
import play.api.http.ContentTypes
import play.api.mvc._

import javax.inject.Inject

class OpenApiController @Inject()(cc: ControllerComponents,
                                  swaggerPlugin: SwaggerPlugin) extends AbstractController(cc) {

  private val AccessControlAllowOrigin: (String, String) = ("Access-Control-Allow-Origin", "*")

  def openApiJson: Action[AnyContent] = Action {
    val response = OpenApiGenerator.generateJson(swaggerPlugin)
    Results
      .Ok(ByteString(response))
      .as(ContentTypes.JSON)
      .withHeaders(AccessControlAllowOrigin)
  }

  def openApiYaml: Action[AnyContent] = Action {
    val response = OpenApiGenerator.generateYaml(swaggerPlugin)
    Results
      .Ok(ByteString(response))
      .as("text/vnd.yaml")
      .withHeaders(AccessControlAllowOrigin)
  }

  def ui: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.openapi.swaggerUi(routes.OpenApiController.openApiJson.absoluteURL()))
  }
}