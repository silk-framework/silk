package controllers.workflow

import controllers.core.UserContextActions
import controllers.util.ProjectUtils._
import controllers.util.SerializationUtils
import controllers.workflow.doc.WorkflowApiDoc
import controllers.workspace.activityApi.StartActivityResponse
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.Task
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.RuleResult
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workbench.workflow.WorkflowWithPayloadExecutor
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow, WorkflowTaskReport}
import play.api.libs.json.{JsArray, JsString, _}
import play.api.mvc.{Action, AnyContent, AnyContentAsXml, _}

import javax.inject.Inject

@Tag(name = "Workflows")
class WorkflowApi @Inject() () extends InjectedController with UserContextActions {

  @deprecated
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

  @deprecated
  def postWorkflow(projectName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    project.addTask[Workflow](workflow.id, workflow)

    Ok
  }

  @deprecated
  def putWorkflow(projectName: String, taskName: String): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val project = fetchProject(projectName)
    implicit val readContext: ReadContext = ReadContext(project.resources, project.config.prefixes)
    val workflow = XmlSerialization.fromXml[Task[Workflow]](request.body.asXml.get.head)
    project.updateTask[Workflow](taskName, workflow)

    Ok
  }

  @deprecated
  def getWorkflow(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    Ok(XmlSerialization.toXml[Task[Workflow]](workflow))
  }

  @deprecated
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

  @deprecated
  def status(projectName: String, taskName: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = fetchProject(projectName)
    val workflow = project.task[Workflow](taskName)
    val report = workflow.activity[LocalWorkflowExecutorGeneratingProvenance].value()

    var lines = Seq[String]()
    lines :+= "Dataset;EntityCount;EntityErrorCount;Column;ColumnErrorCount"

    for {
      WorkflowTaskReport(name, res: TransformReport, _, _) <- report.report.taskReports
      (column, RuleResult(count, _, _, _)) <- res.ruleResults
    } {
      lines :+= s"$name;${res.entityCount};${res.entityErrorCount};$column;$count"
    }

    Ok(lines.mkString("\n"))
  }

  /**
    * Run a variable workflow, where some of the tasks are configured at request time and dataset payload may be
    * delivered inside the request.
    */
  @Operation(
    summary = "Execute workflow with request payload",
    description = WorkflowApiDoc.executeOnPayloadDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadJsonResponseExample))
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadXmlResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      ),
      new ApiResponse(
        responseCode = "503",
        description = "Workflow execution could not be started because concurrent execution limit is reached."
      )
  ))
  @RequestBody(
    description = WorkflowApiDoc.executeOnPayloadBodyDescription,
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadJsonRequestExample))
      ),
      new Content(
        mediaType = "application/xml",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadXmlRequestExample))
      )
    )
  )
  def postVariableWorkflowInput(@Parameter(
                                  name = "project",
                                  description = "The project identifier",
                                  required = true,
                                  in = ParameterIn.PATH,
                                  schema = new Schema(implementation = classOf[String])
                                )
                                projectName: String,
                                @Parameter(
                                  name = "task",
                                  description = "The workflow identifier",
                                  required = true,
                                  in = ParameterIn.PATH,
                                  schema = new Schema(implementation = classOf[String])
                                )
                                workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val resultValue = activity.startBlockingAndGetValue(ParameterValues.fromStringMap(workflowConfiguration))

    SerializationUtils.serializeCompileTime(resultValue, Some(project))
  }

  /**
    * Run a variable workflow in background, where some of the tasks are configured at request time and dataset payload may be
    * delivered inside the request.
    */
  @Operation(
    summary = "Execute workflow with request payload asynchronously",
    description = WorkflowApiDoc.executeOnPayloadAsynchronousDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "Workflow started.",
        headers = Array(
          new Header(
            name = "Location",
            schema = new Schema(example = "/workflow/workflows/projectName/workflowName/execution/ExecuteWorkflowWithPayload14")
          )
        ),
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[StartActivityResponse])
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      ),
      new ApiResponse(
        responseCode = "503",
        description = "Workflow execution could not be started because concurrent execution limit is reached."
      )
    ))
  @RequestBody(
    description = WorkflowApiDoc.executeOnPayloadBodyDescription,
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadJsonRequestExample))
      ),
      new Content(
        mediaType = "application/xml",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject(WorkflowApiDoc.executeOnPayloadXmlRequestExample))
      )
    )
  )
  def postVariableWorkflowInputAsynchronous(@Parameter(
                                              name = "project",
                                              description = "The project identifier",
                                              required = true,
                                              in = ParameterIn.PATH,
                                              schema = new Schema(implementation = classOf[String])
                                            )
                                            projectName: String,
                                            @Parameter(
                                              name = "task",
                                              description = "The workflow identifier",
                                              required = true,
                                              in = ParameterIn.PATH,
                                              schema = new Schema(implementation = classOf[String])
                                            )
                                            workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    implicit val pluginContext: PluginContext = PluginContext.fromProject(project)

    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val id = activity.start(ParameterValues.fromStringMap(workflowConfiguration))
    val result = StartActivityResponse(activity.name, id)

    Created(Json.toJson(result))
        .withHeaders(LOCATION -> controllers.workflow.routes.WorkflowApi.removeVariableWorkflowExecution(projectName, workflowTaskName, id).url)
  }

  @Operation(
    summary = "Remove a workflow execution instance",
    description = WorkflowApiDoc.removeVariableWorkflowExecutionDescription,
      responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the execution instance has been removed."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, workflow or execution instance has not been found."
      )
    ))
  def removeVariableWorkflowExecution(@Parameter(
                                        name = "project",
                                        description = "The project identifier",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        schema = new Schema(implementation = classOf[String])
                                      )
                                      projectName: String,
                                      @Parameter(
                                        name = "task",
                                        description = "The workflow identifier",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        schema = new Schema(implementation = classOf[String])
                                      )
                                      workflowTaskName: String,
                                      @Parameter(
                                        name = "executionId",
                                        description = "The execution identifier",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        schema = new Schema(implementation = classOf[String])
                                      )
                                      workflowExecutionId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContest =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    activity.removeActivityInstance(Identifier(workflowExecutionId))
    NoContent
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