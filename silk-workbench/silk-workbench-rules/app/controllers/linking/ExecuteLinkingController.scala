package controllers.linking

import controllers.core.RequestUserContextAction
import javax.inject.Inject
import org.silkframework.rule.LinkSpec
import org.silkframework.workbench.Context
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class ExecuteLinkingController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def execute(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[LinkSpec](project, task, request.path)
    Ok(views.html.executeLinking.executeLinking(context))
  }

}
