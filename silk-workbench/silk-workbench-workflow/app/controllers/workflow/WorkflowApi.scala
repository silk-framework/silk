package controllers.workflow

import controllers.util.ProjectUtils._
import org.silkframework.dataset.{DataSource, Dataset, SinkTrait}
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.RuleResult
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutor, Workflow}
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json.{JsArray, JsString}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, Controller}
import org.silkframework.config.Task
import org.silkframework.workbench.utils.UnsupportedMediaTypeException

import scala.xml.Elem

class WorkflowApi extends Controller {

  def getWorkflows(projectName: String): Action[AnyContent] = Action {
    val project = fetchProject(projectName)
    val workflowTasks = project.tasks[Workflow]
    val workflowIdsJson = workflowTasks map { task =>
      JsString(task.id.toString)
    }
    Ok(JsArray(workflowIdsJson))
  }

  private def fetchProject(projectName: String) = User().workspace.project(projectName)

  def getWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    Ok(XmlSerialization.toXml[Task[Workflow]](workflow))
  }

  def putWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    project.updateTask[Workflow](taskName, workflow)

    Ok
  }

  def deleteWorkflow(project: String, task: String): Action[AnyContent] = Action {
    User().workspace.project(project).removeTask[Workflow](task)
    Ok
  }

  def executeWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    val activity = workflow.activity[LocalWorkflowExecutor].control
    if (activity.status().isRunning) {
      PreconditionFailed
    } else {
      activity.start()
      Ok
    }
  }

  def status(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    val report = workflow.activity[LocalWorkflowExecutor].value

    var lines = Seq[String]()
    lines :+= "Dataset;EntityCount;EntityErrorCount;Column;ColumnErrorCount"

    for {
      (name, res: TransformReport) <- report.taskReports
      (column, RuleResult(count, _)) <- res.ruleResults
    } {
      lines :+= s"$name;${res.entityCounter};${res.entityErrorCounter};$column;$count"
    }

    Ok(lines.mkString("\n"))
  }

  def postVariableWorkflowInput(projectName: String, workflowTaskName: String): Action[AnyContent] = Action { request =>
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        val workflow = workflowTask.data
        val variableDatasets = workflow.variableDatasets(project)
        // Create data sources from request payload
        val dataSources = {
          // Allow to read from project resources
          implicit val resourceManager: ResourceManager = createInmemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
          createDatasets(xmlRoot, Some(variableDatasets.dataSources.toSet), xmlElementTag = "DataSources")
        }
        // Create sinks and resources for variable datasets, all resources are returned in the response
        val sink2ResourceMap = variableDatasets.sinks.map(s => (s, s + "_file_resource")).toMap
        // Sink with in-memory payload resources only
        implicit val resourceManager: ResourceManager = createInmemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
        val sinks = createDatasets(xmlRoot, Some(sink2ResourceMap.keySet), xmlElementTag = "Sinks")
        executeVariableWorkflow(workflowTask, dataSources, sinks)
        Ok(variableSinkResultXML(resourceManager, sink2ResourceMap))
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml")
    }
  }

  /** Generate result XML for all variable sinks in the workflow. */
  private def variableSinkResultXML(resourceManager: ResourceManager,
                                    sink2ResourceMap: Map[String, String]): Elem = {
    <WorkflowResults>
      {for ((sinkId, resourceId) <- sink2ResourceMap) yield {
      val resource = resourceManager.get(resourceId, mustExist = true)
      <Result sinkId={sinkId}>{resource.loadAsString}</Result>
    }}
    </WorkflowResults>
  }

  private def executeVariableWorkflow(task: ProjectTask[Workflow],
                                      replaceDataSources: Map[String, Dataset],
                                      replaceSinks: Map[String, Dataset]): Unit = {
    val executor = LocalWorkflowExecutor(task, replaceDataSources, replaceSinks, useLocalInternalDatasets = true)
    Activity(executor).startBlocking()
  }
}