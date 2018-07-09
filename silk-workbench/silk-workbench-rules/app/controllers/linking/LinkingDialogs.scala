package controllers.linking

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import play.api.mvc.{Action, AnyContent, Controller}

/** Dialog to configure linking tasks */
class LinkingDialogs extends Controller {

  def linkingTaskDialog(projectName: String, taskName: String, createDialog: Boolean): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    Ok(views.html.dialogs.linkingTaskDialog(projectName, taskName, createDialog))
  }

}