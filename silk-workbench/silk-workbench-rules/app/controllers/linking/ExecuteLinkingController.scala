package controllers.linking

import org.silkframework.rule.LinkSpec
import org.silkframework.workbench.Context
import play.api.mvc.{Action, Controller}

class ExecuteLinkingController  extends Controller {

  def execute(project: String, task: String) = Action { implicit request =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.executeLinking.executeLinking(context))
  }

}
