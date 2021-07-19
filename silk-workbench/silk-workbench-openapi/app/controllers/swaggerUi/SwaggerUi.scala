package controllers.swaggerUi

import com.typesafe.config.{Config, ConfigRenderOptions}
import config.WorkbenchConfig
import controllers.openapi.routes.OpenApi
import controllers.swaggerUi.routes.SwaggerUi
import io.aurora.utils.play.swagger.SwaggerPlugin
import org.silkframework.config.{ConfigValue, DefaultConfig}
import org.silkframework.openapi.OpenApiValidator
import play.api.mvc.{Action, _}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SwaggerUi @Inject()(cc: ControllerComponents, swaggerPlugin: SwaggerPlugin)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  def ui: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.openapi.swaggerUi(
      url = OpenApi.openApiJson.absoluteURL(WorkbenchConfig().useHttps),
      validatorUrl = SwaggerUi.validator(None).absoluteURL(WorkbenchConfig().useHttps),
      config = swaggerUiConfigJson()
    ))
  }

  def validator(url: Option[String]): Action[AnyContent] = Action { implicit request =>
    val result = OpenApiValidator.validate(swaggerPlugin, url)
    if(result.messages.isEmpty) {
      Ok.sendResource("icons/valid.png")
    } else {
      Ok.sendResource("icons/invalid.png")
    }
  }

  def validatorDebug(url: Option[String]): Action[AnyContent] = Action { implicit request =>
    Redirect(OpenApi.validate(url).absoluteURL(WorkbenchConfig().useHttps))
  }

  private val swaggerUiConfigJson: ConfigValue[String] = (config: Config) => {
    val allPluginsConf = config.getObject("swagger.ui")
    val json = allPluginsConf.render(ConfigRenderOptions.concise())
    json
  }
}