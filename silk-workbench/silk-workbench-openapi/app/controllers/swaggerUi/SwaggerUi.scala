package controllers.swaggerUi

import controllers.openapi.routes.OpenApi
import controllers.swaggerUi.routes.SwaggerUi
import org.silkframework.openapi.OpenApiValidator
import org.silkframework.runtime.validation.BadUserInputException
import play.api.libs.json.Json
import play.api.mvc.{Action, _}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SwaggerUi @Inject()(cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  def ui: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.openapi.swaggerUi(
      url = OpenApi.openApiJson.absoluteURL(),
      validatorUrl = SwaggerUi.validator(None).absoluteURL()
    ))
  }

  def validator(url: Option[String]): Action[AnyContent] = Action {
    url match {
      case Some(url) =>
        val result = OpenApiValidator.validate(url)
        if(result.messages.isEmpty) {
          Ok.sendResource("icons/valid.png")
        } else {
          Ok.sendResource("icons/invalid.png")
        }
      case None =>
        throw new BadUserInputException("Parameter 'url' is missing.")
    }
  }

  def validatorDebug(url: Option[String]): Action[AnyContent] = Action { implicit request =>
    url match {
      case Some(u) =>
        Redirect(OpenApi.validate(u).absoluteURL())
      case None =>
        throw new BadUserInputException("Parameter 'url' is missing.")
    }
  }
}