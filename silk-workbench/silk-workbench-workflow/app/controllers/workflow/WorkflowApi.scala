package controllers.workflow

import controllers.util.ProjectUtils._
import controllers.workspace.WorkspaceApi._
import org.silkframework.dataset.DataSource
import org.silkframework.execution.ExecuteTransformResult
import org.silkframework.execution.ExecuteTransformResult.RuleResult
import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.{Task, User}
import org.silkframework.workspace.activity.workflow.{WorkflowExecutorFactory, Workflow, WorkflowExecutionReport, WorkflowExecutor}
import play.api.mvc.{AnyContentAsXml, Action, Controller}

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
      (column, RuleResult(count, _)) <- res.ruleResults
    } {
      lines :+= s"$name;${res.entityCounter};${res.entityErrorCounter};$column;$count"
    }

    Ok(lines.mkString("\n"))
  }

  /**
    * Handles POST requests for variable workflows, i.e. all data for all variable data sources comes directly with
    * the request.
    *
    * @param projectName
    * @param workflowTaskName
    * @return
    */
  def postVariableWorkflowInput(projectName: String, workflowTaskName: String) = Action { request =>
    val (_, workflow) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        implicit val resourceManager = createInmemoryResourceManagerForResources(xmlRoot)
        val dataSources = createDataSources(xmlRoot)
        val (model, entitySink) = createEntitySink(xmlRoot)
        executeVariableWorkflow(workflow, dataSources)
        val acceptedContentType = request.acceptedTypes.headOption.map(_.toString()).getOrElse("application/n-triples")
        result(model, acceptedContentType, "Data transformed successfully!")
      case _ =>
        UnsupportedMediaType("Only XML supported")
    }
  }

  private def executeVariableWorkflow(task: Task[Workflow],
                                      replaceDataSources: Map[String, DataSource]): Unit = {
    val executor = new WorkflowExecutor(task, replaceDataSources)
    Activity(executor).startBlocking()
  }
}