package controllers.core

import org.silkframework.config.DefaultConfig
import play.api.mvc.{Action, Controller}

class ConfigController extends Controller {

  private val ignoredPaths = Set("awt", "file", "jline", "line", "path", "promise", "sbt", "play.crypto.secret")

  def index = Action { implicit request =>
    var config = DefaultConfig.instance()
    for(path <- ignoredPaths) {
      config = config.withoutPath(path)
    }

    Ok(views.html.configView(config))
  }

}
