package controllers.linking

import controllers.core.RequestUserContextAction
import org.silkframework.rule.LinkSpec
import org.silkframework.workbench.Context
import play.api.mvc.{Action, AnyContent, Controller}

class ExecuteLinkingController  extends Controller {

  def execute(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.executeLinking.executeLinking(context))
  }

}
