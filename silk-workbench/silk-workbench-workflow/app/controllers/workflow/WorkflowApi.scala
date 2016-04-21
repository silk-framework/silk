package controllers.workflow

import org.silkframework.execution.ExecuteTransformResult
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutionReport, WorkflowExecutor}
import play.api.mvc.{Action, Controller}

object WorkflowApi extends Controller {

  def getWorkflow(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val workflow = project.task[Workflow](taskName)

    Ok(workflow.data.toXML)
  }

  def putWorkflow(projectName: String, taskName: String) = Action { request =>
    val project = User().workspace.project(projectName)
    val workflow = Workflow.fromXML(request.body.asXml.get.head).copy(id = taskName)
    project.updateTask[Workflow](taskName, workflow)

    Ok
  }

  def deleteWorkflow(project: String, task: String) = Action {
    User().workspace.project(project).removeTask[Workflow](task)
    Ok
  }

  def executeWorkflow(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val workflow = project.task[Workflow](taskName)
    val activity = workflow.activity[WorkflowExecutor].control
    if(activity.status().isRunning)
      PreconditionFailed
    else {
      activity.start()
      Ok
    }
  }

  def status(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val workflow = project.task[Workflow](taskName)
    val report = workflow.activity[WorkflowExecutor].value

    var lines = Seq[String]()
    lines :+= "Dataset;EntityCount;EntityErrorCount;Column;ColumnErrorCount"

    for{
      (name, res: ExecuteTransformResult) <- report.taskReports
      (column, count) <- res.ruleErrorCounter
    } {
      lines :+= s"$name;${res.entityCounter};${res.entityErrorCounter};$column;$count"
    }

    Ok(lines.mkString("\n"))
  }

}