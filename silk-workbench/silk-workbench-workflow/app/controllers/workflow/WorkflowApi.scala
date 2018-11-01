package controllers.workflow

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils
import org.silkframework.config.{MetaData, Task}
import org.silkframework.dataset.Dataset
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.RuleResult
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workbench.workflow.WorkflowWithPayloadExecutor
import org.silkframework.workspace.activity.workflow._
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, Controller, _}

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
                                workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    activity.startBlocking(workflowConfiguration)

    SerializationUtils.serializeCompileTime(activity.value)
  }

  def postVariableWorkflowInput2(projectName: String,
                                workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val id = activity.startBlocking(workflowConfiguration)

    Ok(Json.obj(("activityId", id.toString)))
  }

  private def workflowConfiguration(implicit request: Request[AnyContent]): Map[String, String] = {
    request.body match {
      case AnyContentAsXml(xmlRoot) =>
        Map("configuration" -> xmlRoot.toString, "configurationType" -> "application/xml")
      case AnyContentAsJson(json) =>
        Map("configuration" -> json.toString, "configurationType" -> "application/json")
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml", "application/json")
    }
  }
}