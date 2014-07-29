package controllers.workflow

import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.{WorkflowExecutor, WorkflowTask}
import models.CurrentExecutionTask
import play.api.mvc.{Action, Controller}

object WorkflowApi extends Controller {

  def getWorkflow(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val workflow = project.task[WorkflowTask](taskName)

    Ok(workflow.toXML)
  }

  def putWorkflow(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val workflow = WorkflowTask.fromXML(taskName, request.body.asXml.get.head, project)
    project.updateTask[WorkflowTask](workflow)

    Ok
  }

  def executeWorkflow(projectName: String, taskName: String) = Action {
    if(CurrentExecutionTask().status.isRunning)
      PreconditionFailed
    else {
      val project = User().workspace.project(projectName)
      val workflow = project.task[WorkflowTask](taskName)
      val executor = new WorkflowExecutor(workflow.operators, project)

      CurrentExecutionTask() = executor()
      CurrentExecutionTask().runInBackground()
      Ok
    }
  }
}