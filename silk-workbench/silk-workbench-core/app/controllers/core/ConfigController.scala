package controllers.core

import javax.inject.Inject
import org.silkframework.config.DefaultConfig
import play.api.mvc.{InjectedController, ControllerComponents}

class ConfigController @Inject() () extends InjectedController {

  private val ignoredPaths = Set("awt", "file", "jline", "line", "path", "promise", "sbt", "play.http.secret.key")

  def index = Action { implicit request =>
    var config = DefaultConfig.instance()
    for(path <- ignoredPaths) {
      config = config.withoutPath(path)
    }

    Ok(views.html.configView(config))
  }

}
