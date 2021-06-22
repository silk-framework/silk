package controllers.openapi

import io.aurora.utils.play.swagger.SwaggerPlugin
import play.api.mvc._

import javax.inject.Inject

class OpenApiController @Inject()(cc: ControllerComponents,
                                  swaggerPlugin: SwaggerPlugin) extends AbstractController(cc) {

  def apiSpec: Action[AnyContent] = {
    new io.aurora.utils.play.swagger.controllers.SwaggerController(cc, swaggerPlugin).getResourcesAsYaml
  }

  def ui: Action[AnyContent] = Action { implicit request =>
    Redirect("api/index.html?url=" + routes.OpenApiController.apiSpec.absoluteURL())
  }
}