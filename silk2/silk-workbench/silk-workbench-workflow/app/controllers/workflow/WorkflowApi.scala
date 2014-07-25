package controllers.workflow

import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowTask
import play.api.mvc.{Action, Controller}

object WorkflowApi extends Controller {

  def workflow(projectName: String, taskName: String) = Action { request =>

    println(request.body.asXml)

    val project = User().workspace.project(projectName)
    val workflow = WorkflowTask.fromXML(taskName, request.body.asXml.get.head, project)
    project.updateTask[WorkflowTask](workflow)

    Ok
  }
}