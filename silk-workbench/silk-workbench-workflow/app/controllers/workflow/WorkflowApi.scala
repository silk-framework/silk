package controllers.workflow

import controllers.util.ProjectUtils._
import org.silkframework.config.{MetaData, Task}
import org.silkframework.dataset.Dataset
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.RuleResult
import org.silkframework.runtime.activity.{Activity, SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.users.{WebUser, WebUserManager}
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workspace.activity.workflow._
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.mvc._

import scala.xml.{Elem, NodeSeq}

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

  def postWorkflow(projectName: String): Action[AnyContent] = Action { request =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    project.addTask[Workflow](workflow.id, workflow)

    Ok
  }

  def putWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    // The workflow that is sent to this endpoint by the editor does not contain the metadata
    val metaData = project.anyTaskOption(taskName).map(_.metaData).getOrElse(MetaData.empty)
    project.updateTask[Workflow](taskName, workflow, metaData)

    Ok
  }

  def getWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action {
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    Ok(XmlSerialization.toXml[Task[Workflow]](workflow))
  }

  def deleteWorkflow(project: String, task: String): Action[AnyContent] = Action {
    User().workspace.project(project).removeTask[Workflow](task)
    Ok
  }

  def executeWorkflow(projectName: String, taskName: String): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = SimpleUserContext(WebUserManager().user(request))
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

  def status(projectName: String, taskName: String): Action[AnyContent] = Action {
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
                                workflowTaskName: String): Action[AnyContent] = Action { request =>
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    val workflow = workflowTask.data
    val variableDatasets = workflow.variableDatasets(project)
    // Create sinks and resources for variable datasets, all resources are returned in the response
    val variableSinks = variableDatasets.sinks
    val webUser = WebUserManager.instance.user(request)
    val (dataSources, sinks, resultResourceManager) = request.body match {
      case AnyContentAsXml(xmlRoot) =>
        createSourcesSinksFromXml(projectName, variableDatasets, variableSinks.toSet, xmlRoot)
      case AnyContentAsJson(json) =>
        createSourceSinksFromJson(projectName, variableDatasets, variableSinks.toSet, json)
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml", "application/json")
    }
    val sink2ResourceMap = variableSinks.map(s => (s, sinks.get(s).flatMap(_.parameters.get("file")).getOrElse(s + "_file_resource"))).toMap
    executeVariableWorkflow(workflowTask, dataSources, sinks, webUser)
    variableSinkResult(resultResourceManager, sink2ResourceMap, request)
  }

  private def createSourceSinksFromJson(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], json: JsValue) = {
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

  private def createSourcesSinksFromXml(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], xmlRoot: NodeSeq) = {
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
                                      replaceSinks: Map[String, Dataset],
                                      webUser: Option[WebUser]): Unit = {
    val executor = LocalWorkflowExecutorGeneratingProvenance(task, replaceDataSources, replaceSinks, useLocalInternalDatasets = true)
    val activityExecution = Activity(executor)
    implicit val userContext: UserContext = SimpleUserContext(webUser)
    activityExecution.startBlocking()
  }
}