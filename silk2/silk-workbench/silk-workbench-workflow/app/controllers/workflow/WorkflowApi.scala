package controllers.workflow

import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.{WorkflowExecutor, Workflow}
import models.CurrentExecutionTask
import play.api.mvc.{Action, Controller}

object WorkflowApi extends Controller {

  def getWorkflow(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val workflow = project.task[Workflow](taskName)

    Ok(workflow.data.toXML)
  }

  def putWorkflow(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val workflow = Workflow.fromXML(request.body.asXml.get.head, project)
    project.updateTask[Workflow](taskName, workflow)

    Ok
  }

  def deleteWorkflow(project: String, task: String) = Action {
    User().workspace.project(project).removeTask[Workflow](task)
    Ok
  }

  def executeWorkflow(projectName: String, taskName: String) = Action {
    if(CurrentExecutionTask().status().isRunning)
      PreconditionFailed
    else {
      val project = User().workspace.project(projectName)
      val workflow = project.task[Workflow](taskName).data
      val executor = new WorkflowExecutor(workflow.operators, project)
      val taskControl = Activity.execute(executor())
      CurrentExecutionTask() = taskControl
      Ok
    }
  }
}