package controllers.transform

import controllers.core.RequestUserContextAction
import javax.inject.Inject
import org.silkframework.rule.TransformSpec
import org.silkframework.workbench.Context
import org.silkframework.workbench.workspace.WorkbenchAccessMonitor
import play.api.mvc.{Action, AnyContent, InjectedController}

/** Endpoints for the 'Execute' page of a transform task */
class ExecuteTransformTab @Inject() (accessMonitor: WorkbenchAccessMonitor) extends InjectedController {

  def execute(project: String, task: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    accessMonitor.saveProjectTaskAccess(project, task)
    Ok(views.html.executeTransform.executeTransform(context))
  }

  def executionReport(project: String, task: String): Action[AnyContent] = RequestUserContextAction { request =>implicit userContext =>
    val context = Context.get[TransformSpec](project, task, request.path)
    Ok(views.html.executeTransform.transformReport(context.task))
  }

}
