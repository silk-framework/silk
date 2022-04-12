package controllers.core

import config.WorkbenchConfig
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.twirl.api.Html

import javax.inject.Inject

class Start @Inject() () extends InjectedController with UserContextActions {

  def index: Action[AnyContent] = RequestUserContextAction { implicit request =>implicit userContext =>
    val welcome = Html(WorkbenchConfig.get.welcome.loadAsString())
    Ok(views.html.start(welcome))
  }
}

object Start {

  def deployPath: String = {
    val loggedOutFull = controllers.core.routes.Start.index.url.toString
    val path = loggedOutFull.dropRight("core/start".length)
    if(path.isEmpty) {
      "/"
    } else {
      path
    }
  }

}
