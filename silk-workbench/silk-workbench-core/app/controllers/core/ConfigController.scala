package controllers.core

import javax.inject.Inject
import org.silkframework.config.{DefaultConfig, ProductionConfig}
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.{Environment, Mode}

class ConfigController @Inject()(env: Environment) extends InjectedController {

  private val ignoredPaths = Set("awt", "file", "jline", "line", "path", "promise", "sbt", "play.http.secret.key",
    "plugin.parameters.password.crypt.key", "oauth.clientSecret", "workbench.superuser", "play.server.https.keyStore.password")

  def index: Action[AnyContent] = Action { implicit request =>
    var config = DefaultConfig.instance()
    for (path <- ignoredPaths) {
      config = config.withoutPath(path)
    }
    if (env.mode == Mode.Prod) {
      NotFound("Config endpoint is not available in production mode.")
    } else {
      Ok(views.html.configView(config))
    }
  }

  def setSafeMode(enable: Boolean): Action[AnyContent] = Action {
    ProductionConfig.setSafeMode(enable)
    Ok("Safe-mode set to " + enable)
  }

  def safeMode(): Action[AnyContent] = Action {
    val result = JsObject(Seq(
      "safeModeEnabled" -> JsBoolean(ProductionConfig.safeModeEnabled)
    ) ++ Seq("inSafeMode" -> JsBoolean(ProductionConfig.inSafeMode)).filter(_ => ProductionConfig.safeModeEnabled)) // don't show when safe mode is disabled
    Ok(result)
  }
}
