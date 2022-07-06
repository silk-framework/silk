package controllers.core

import config.WorkbenchConfig.WorkspaceReact
import org.silkframework.config.{DefaultConfig, ProductionConfig}
import play.api.Environment
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

class ConfigController @Inject()(implicit workspaceReact: WorkspaceReact) extends InjectedController {

  private val ignoredPaths = Set("awt", "file", "jline", "line", "path", "promise", "sbt", "play.http.secret.key",
    "plugin.parameters.password.crypt.key", "oauth.clientSecret", "workbench.superuser", "play.server.https.keyStore.password")

  private val CONFIG_VIEW_ENABLED = "workbench.configView.enabled"

  def index: Action[AnyContent] = Action { implicit request =>
    var config = DefaultConfig.instance()
    for (path <- ignoredPaths) {
      config = config.withoutPath(path)
    }
    if (config.hasPath(CONFIG_VIEW_ENABLED) && config.getBoolean(CONFIG_VIEW_ENABLED)) {
      Ok(views.html.configView(config))
    } else {
      NotFound(s"Config view is disabled. It can be enabled by setting '$CONFIG_VIEW_ENABLED'.")
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
