package controllers.workflow

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.ProjectUtils._
import org.silkframework.config.{MetaData, Task}
import org.silkframework.dataset.Dataset
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.RuleResult
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workspace.activity.workflow.{AllVariableDatasets, LocalWorkflowExecutorGeneratingProvenance, Workflow}
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, Controller}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.mvc._

import scala.xml.NodeSeq

class WorkflowApi extends Controller {

  def getWorkflows(projectName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflowTasks = project.tasks[Workflow]
    val workflowIdsJson = workflowTasks map { task =>
      JsString(task.id.toString)
    }
    Ok(JsArray(workflowIdsJson))
  }

  private def fetchProject(projectName: String)
                          (implicit userContext: UserContext) = WorkspaceFactory().workspace.project(projectName)

  def postWorkflow(projectName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    project.addTask[Workflow](workflow.id, workflow)

    Ok
  }

  def putWorkflow(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    // The workflow that is sent to this endpoint by the editor does not contain the metadata
    val metaData = project.anyTaskOption(taskName).map(_.metaData).getOrElse(MetaData.empty)
    project.updateTask[Workflow](taskName, workflow, metaData)

    Ok
  }

  def getWorkflow(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    Ok(XmlSerialization.toXml[Task[Workflow]](workflow))
  }

  def deleteWorkflow(project: String, task: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    WorkspaceFactory().workspace.project(project).removeTask[Workflow](task)
    Ok
  }

  def executeWorkflow(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    val activity = workflow.activity[LocalWorkflowExecutorGeneratingProvenance].control
    if (activity.status().isRunning) {
      PreconditionFailed
    } else {
      activity.start()
      Ok
    }
  }

  def status(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    val report = workflow.activity[LocalWorkflowExecutorGeneratingProvenance].value

    var lines = Seq[String]()
    lines :+= "Dataset;EntityCount;EntityErrorCount;Column;ColumnErrorCount"

    for {
      (name, res: TransformReport) <- report.report.taskReports
      (column, RuleResult(count, _)) <- res.ruleResults
    } {
      lines :+= s"$name;${res.entityCounter};${res.entityErrorCounter};$column;$count"
    }

    Ok(lines.mkString("\n"))
  }

  /**
    * Run a variable workflow, where some of the tasks are configured at request time and dataset payload may be
    * delivered inside the request.
    */
  def postVariableWorkflowInput(projectName: String,
                                workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    val variableDatasets = workflowTask.data.variableDatasets(project)

    // Create sinks and resources for variable datasets, all resources are returned in the response
    val variableSinks = variableDatasets.sinks
    val (dataSources, sinks, resultResourceManager) = request.body match {
      case AnyContentAsXml(xmlRoot) =>
        createSourcesSinksFromXml(projectName, variableDatasets, variableSinks.toSet, xmlRoot)
      case AnyContentAsJson(json) =>
        createSourceSinksFromJson(projectName, variableDatasets, variableSinks.toSet, json)
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml", "application/json")
    }
    val sink2ResourceMap = sinkToResourceMapping(sinks, variableSinks)
    executeVariableWorkflow(workflowTask, dataSources, sinks)
    variableSinkResult(resultResourceManager, sink2ResourceMap, request)
  }

  private def sinkToResourceMapping(sinks: Map[String, Dataset], variableSinks: Seq[String]) = {
    variableSinks.map(s =>
      s -> sinks.get(s).flatMap(_.parameters.get("file")).getOrElse(s + "_file_resource")
    ).toMap
  }

  private def createSourceSinksFromJson(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], json: JsValue)
                                       (implicit userContext: UserContext): (Map[String, Dataset], Map[String, Dataset], ResourceManager) = {
    val workflowJson = json.as[JsObject]
    val dataSources = {
      implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
      createDatasets(workflowJson, Some(variableDatasets.dataSources.toSet), property = "DataSources")
    }
    // Sink
    val (sinkResourceManager, resultResourceManager) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
    implicit val resourceManager: ResourceManager = sinkResourceManager
    val sinks = createDatasets(workflowJson, Some(sinkIds), property = "Sinks")
    (dataSources, sinks, resultResourceManager)
  }

  private def createSourcesSinksFromXml(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], xmlRoot: NodeSeq)
                                       (implicit userContext: UserContext): (Map[String, Dataset], Map[String, Dataset], ResourceManager) = {
    // Create data sources from request payload
    val dataSources = {
      // Allow to read from project resources
      implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
      createDatasets(xmlRoot, Some(variableDatasets.dataSources.toSet), xmlElementTag = "DataSources")
    }
    // Sink
    val (sinkResourceManager, resultResourceManager) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
    implicit val resourceManager: ResourceManager = sinkResourceManager
    val sinks = createDatasets(xmlRoot, Some(sinkIds), xmlElementTag = "Sinks")
    (dataSources, sinks, resultResourceManager)
  }

  /** Generate result XML for all variable sinks in the workflow. */
  private def variableSinkResult(resourceManager: ResourceManager,
                                 sink2ResourceMap: Map[String, String],
                                 request: Request[AnyContent]): Result = {
    if(request.accepts("application/xml")) {
      Ok(variableSinkResultXml(resourceManager, sink2ResourceMap))
    } else if(request.accepts("application/json")) {
      Ok(variableSinkResultJson(resourceManager, sink2ResourceMap))
    } else {
      Ok(variableSinkResultXml(resourceManager, sink2ResourceMap))
    }
  }

  private def variableSinkResultXml(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]) = {
    <WorkflowResults>
      {for ((sinkId, resourceId) <- sink2ResourceMap if resourceManager.exists(resourceId)) yield {
      val resource = resourceManager.get(resourceId, mustExist = true)
      <Result sinkId={sinkId}>{resource.loadAsString}</Result>
    }}
    </WorkflowResults>
  }

  private def variableSinkResultJson(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]) = {
    JsArray(
      for ((sinkId, resourceId) <- sink2ResourceMap.toSeq if resourceManager.exists(resourceId)) yield {
        val resource = resourceManager.get(resourceId, mustExist = true)
        JsObject(Seq(
          "sinkId" -> JsString(sinkId),
          "textContent" -> JsString(resource.loadAsString)
        ))
      }
    )
  }

  private def executeVariableWorkflow(task: ProjectTask[Workflow],
                                      replaceDataSources: Map[String, Dataset],
                                      replaceSinks: Map[String, Dataset])
                                     (implicit userContext: UserContext): Unit = {
    val executor = LocalWorkflowExecutorGeneratingProvenance(task, replaceDataSources, replaceSinks, useLocalInternalDatasets = true)
    val activityExecution = Activity(executor)
    activityExecution.startBlocking()
  }
}