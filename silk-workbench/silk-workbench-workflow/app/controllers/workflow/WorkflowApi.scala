package controllers.workflow

import controllers.util.ProjectUtils._
import org.silkframework.dataset.{SinkTrait, DataSource}
import org.silkframework.execution.ExecuteTransformResult
import org.silkframework.execution.ExecuteTransformResult.RuleResult
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowExecutor}
import org.silkframework.workspace.{Task, User}
import play.api.mvc.{Action, AnyContentAsXml, Controller}

import scala.xml.Elem

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
    if (activity.status().isRunning)
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

    for {
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
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        val workflow = workflowTask.data
        val variableDatasets = workflow.variableDatasets(project)
        implicit val resourceManager = createInmemoryResourceManagerForResources(xmlRoot)
        // Create data sources from request payload
        val dataSources = createDataSources(xmlRoot, Some(variableDatasets.dataSources.toSet))
        // Create sinks and resources for variable datasets, all resources are returned in the response
        val sink2ResourceMap = variableDatasets.sinks.map(s => (s, s + "_file_resource")).toMap
        val sinks = createInMemorySink(xmlRoot, sink2ResourceMap)
        executeVariableWorkflow(workflowTask, dataSources, sinks)
        Ok(variableSinkResultXML(resourceManager, sink2ResourceMap))
      case _ =>
        UnsupportedMediaType("Only XML supported")
    }
  }

  /** Generate result XML for all variable sinks in the workflow. */
  private def variableSinkResultXML(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]): Elem = {
    <WorkflowResults>
      {for ((sinkId, resourceId) <- sink2ResourceMap) yield {
      val resource = resourceManager.get(resourceId, mustExist = true)
      <Result sinkId={sinkId}>{resource.loadAsString}</Result>
    }}
    </WorkflowResults>
  }

  private def executeVariableWorkflow(task: Task[Workflow],
                                      replaceDataSources: Map[String, DataSource],
                                      replaceSinks: Map[String, SinkTrait]): Unit = {
    val executor = new WorkflowExecutor(task, replaceDataSources, replaceSinks)
    Activity(executor).startBlocking()
  }
}