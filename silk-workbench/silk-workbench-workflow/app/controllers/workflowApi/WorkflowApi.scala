package controllers.workflowApi

import akka.util.ByteString
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.ProjectUtils.getProjectAndTask
import controllers.workflowApi.doc.WorkflowApiDoc
import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils
import controllers.workflowApi.workflow.{WorkflowInfo, WorkflowNodePortConfig, WorkflowNodesPortConfig}
import controllers.workspace.activityApi.StartActivityResponse
import controllers.workspaceApi.search.ItemType
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.CustomTask
import org.silkframework.runtime.validation.{NotFoundException, RequestException}
import org.silkframework.workbench.workflow.WorkflowWithPayloadExecutor
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc._

import java.net.HttpURLConnection
import javax.inject.Inject

@Tag(name = "Workflows", description = "Workflow specific operations, such as execution of workflows with payloads.")
class WorkflowApi @Inject()() extends InjectedController with ControllerUtilsTrait with UserContextActions {

  @Operation(
    summary = "Parameterized workflow execution result",
    description = WorkflowApiDoc.variableWorkflowResultGetDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the workflow was successful. The result of the variable output dataset is returned in the response.",
        content = Array(
          new Content(
            mediaType = "application/json"
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseXmlExample))
          ),
          new Content(
            mediaType = "application/n-triples",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseNTriplesExample))
          ),
          new Content(
            mediaType = "text/csv",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseCsvExample))
          ),
          new Content(
            mediaType = "text/comma-separated-values",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseCsvExample))
          ),
          new Content(
            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          )
        )
      ),
      new ApiResponse(
        responseCode = "204",
        description = "If the workflow was successful, but contains no variable output dataset."
      ),
      new ApiResponse(
        responseCode = "400",
        description = " Invalid request, e.g. no request parameters provided."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      ),
      new ApiResponse(
        responseCode = "406",
        description = "If no response in any of the requested mime types could be produced."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "The workflow execution has failed."
      )
  ))
  def variableWorkflowResultGet(@Parameter(
                                  name = "projectId",
                                  description = "The project identifier",
                                  required = true,
                                  in = ParameterIn.PATH,
                                  schema = new Schema(implementation = classOf[String])
                                )
                                projectName: String,
                                @Parameter(
                                  name = "workflowId",
                                  description = "The workflow identifier",
                                  required = true,
                                  in = ParameterIn.PATH,
                                  schema = new Schema(implementation = classOf[String])
                                )
                                workflowTaskName: String): Action[AnyContent] = {
    variableWorkflowResult(projectName, workflowTaskName)
  }

  @Operation(
    summary = "Parameterized workflow execution result",
    description = WorkflowApiDoc.variableWorkflowResultPostDescription,
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the workflow was successful. The result of the variable output dataset is returned in the response.",
        content = Array(
          new Content(
            mediaType = "application/json"
          ),
          new Content(
            mediaType = "application/xml",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseXmlExample))
          ),
          new Content(
            mediaType = "application/n-triples",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseNTriplesExample))
          ),
          new Content(
            mediaType = "text/csv",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseCsvExample))
          ),
          new Content(
            mediaType = "text/comma-separated-values",
            examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowResponseCsvExample))
          ),
          new Content(
            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          )
        )
      ),
      new ApiResponse(
        responseCode = "204",
        description = "If the workflow was successful, but contains no variable output dataset."
      ),
      new ApiResponse(
        responseCode = "400",
        description = " Invalid request, e.g. no request parameters provided."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      ),
      new ApiResponse(
        responseCode = "406",
        description = "If no response in any of the requested mime types could be produced."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "The workflow execution has failed."
      )
  ))
  @RequestBody(
    description = "The contents of the variable data source.",
    required = false,
    content = Array(
      new Content(
        mediaType = "application/x-www-form-urlencoded",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "Multiple values are provided for an input property by having the same parameter multiple times in the request body.",
            value = WorkflowApiDoc.variableWorkflowRequestFormsExample
        ))
      ),
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(
          new ExampleObject(
            value = WorkflowApiDoc.variableWorkflowRequestJsonExample
        ))
      ),
      new Content(
        mediaType = "application/xml",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            value = WorkflowApiDoc.variableWorkflowRequestXmlExample
        ))
      ),
      new Content(
        mediaType = "text/comma-separated-values",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "The CSV format is the default CSV dataset config, e.g. there is no array separator defined.",
            value = WorkflowApiDoc.variableWorkflowRequestCsvExample
        ))
      ),
      new Content(
        mediaType = "text/csv",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "The CSV format is the default CSV dataset config, e.g. there is no array separator defined.",
            value = WorkflowApiDoc.variableWorkflowRequestCsvExample
        ))
      )
    )
  )
  def variableWorkflowResultPost(@Parameter(
                                   name = "projectId",
                                   description = "The project identifier",
                                   required = true,
                                   in = ParameterIn.PATH,
                                   schema = new Schema(implementation = classOf[String])
                                 )
                                 projectName: String,
                                 @Parameter(
                                   name = "workflowId",
                                   description = "The workflow identifier",
                                   required = true,
                                   in = ParameterIn.PATH,
                                   schema = new Schema(implementation = classOf[String])
                                 )
                                 workflowTaskName: String): Action[AnyContent] = {
    variableWorkflowResult(projectName, workflowTaskName)
  }

  /**
    * Run a variable workflow, where some of the tasks are configured at request time and dataset payload may be
    * delivered inside the request.
    */
  private def variableWorkflowResult(projectName: String,
                                     workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val (workflowConfig, mimeTypeOpt) = VariableWorkflowRequestUtils.queryStringToWorkflowConfig(project, workflowTask)
    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val resultValue = activity.startBlockingAndGetValue(workflowConfig)
    mimeTypeOpt match {
      case Some(mimeType) =>
        val outputResource = resultValue.resourceManager.get(VariableWorkflowRequestUtils.OUTPUT_FILE_RESOURCE_NAME, mustExist = true)
        Result(
          header = ResponseHeader(OK, Map.empty),
          body = HttpEntity.Strict(ByteString(outputResource.loadAsBytes), Some(mimeType))
        )
      case None =>
        NoContent
    }
  }

  @Operation(
    summary = "Parameterized workflow execution result (asynchronous)",
    description = WorkflowApiDoc.variableWorkflowResultPostDescriptionAsync,
    parameters = Array(
      new Parameter(
        name = VariableWorkflowRequestUtils.QUERY_PARAM_OUTPUT_TYPE,
        description = "The type of the output dataset",
        required = true,
        in = ParameterIn.QUERY,
        schema = new Schema(implementation = classOf[String], example = "csv")
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "201",
        description = "If the workflow has been started successfully.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[StartActivityResponse])
          )
        )
      ),
      new ApiResponse(
        responseCode = "400",
        description = " Invalid request, e.g. no request parameters provided."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      ),
      new ApiResponse(
        responseCode = "406",
        description = "If no response in any of the requested mime types could be produced."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "The workflow execution has failed."
      )
    ))
  @RequestBody(
    description = "The contents of the variable data source.",
    required = false,
    content = Array(
      new Content(
        mediaType = "application/x-www-form-urlencoded",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "Multiple values are provided for an input property by having the same parameter multiple times in the request body.",
            value = WorkflowApiDoc.variableWorkflowRequestFormsExample
          ))
      ),
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowRequestJsonExample))
      ),
      new Content(
        mediaType = "application/xml",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(new ExampleObject(WorkflowApiDoc.variableWorkflowRequestXmlExample))),
      new Content(
        mediaType = "text/comma-separated-values",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "The CSV format is the default CSV dataset config, e.g. there is no array separator defined.",
            value = WorkflowApiDoc.variableWorkflowRequestCsvExample
          ))
      ),
      new Content(
        mediaType = "text/csv",
        schema = new Schema(implementation = classOf[String]),
        examples = Array(
          new ExampleObject(
            description = "The CSV format is the default CSV dataset config, e.g. there is no array separator defined.",
            value = WorkflowApiDoc.variableWorkflowRequestCsvExample
          ))
      )
    )
  )
  def executeVariableWorkflowAsync(@Parameter(
                                     name = "projectId",
                                     description = "The project identifier",
                                     required = true,
                                     in = ParameterIn.PATH,
                                     schema = new Schema(implementation = classOf[String])
                                   )
                                   projectName: String,
                                   @Parameter(
                                     name = "workflowId",
                                     description = "The workflow identifier",
                                     required = true,
                                     in = ParameterIn.PATH,
                                     schema = new Schema(implementation = classOf[String])
                                   )
                                   workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)
    val (workflowConfig, _) = VariableWorkflowRequestUtils.queryStringToWorkflowConfig(project, workflowTask)
    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val id = activity.start(workflowConfig)
    val result = StartActivityResponse(activity.name, id)
    Created(Json.toJson(result))
  }

  @Operation(
    summary = "Parameterized workflow execution result (asynchronous)",
    description = "Returns the result of a workflow execution.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the workflow was successful. The result of the variable output dataset is returned in the response."
      ),
      new ApiResponse(
        responseCode = "204",
        description = "If the workflow was successful, but contains no variable output dataset."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the workflow is still running and did not produce a result yet."
      ),
      new ApiResponse(
        responseCode = "500",
        description = "If the workflow has not completed successfully."
      )
    )
  )
  def variableWorkflowAsyncResult(@Parameter(
                                    name = "projectId",
                                    description = "The project identifier",
                                    required = true,
                                    in = ParameterIn.PATH,
                                    schema = new Schema(implementation = classOf[String])
                                  )
                                  projectName: String,
                                  @Parameter(
                                    name = "workflowId",
                                    description = "The workflow identifier",
                                    required = true,
                                    in = ParameterIn.PATH,
                                    schema = new Schema(implementation = classOf[String])
                                  )
                                  workflowTaskName: String,
                                  @Parameter(
                                    name = "instanceId",
                                    description = "The activity instance identifier",
                                    required = true,
                                    in = ParameterIn.PATH,
                                    schema = new Schema(implementation = classOf[String])
                                  )
                                  instanceId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    // Make sure that the activity has been successful
    val activity = workflowTask.activity[WorkflowWithPayloadExecutor].instance(instanceId)
    if(activity.status().isRunning) {
      throw NotFoundException("Workflow is still running.")
    } else if(activity.status().failed) {
      throw WorkflowFailedException("Cannot retrieve workflow result, because execution failed.", activity.status().exception)
    }

    // Return output
    val outputResource = activity.value().resourceManager.get(VariableWorkflowRequestUtils.OUTPUT_FILE_RESOURCE_NAME, mustExist = true)
    Result(
      header = ResponseHeader(OK, Map.empty),
      body = HttpEntity.Strict(ByteString(outputResource.loadAsBytes), None)
    )
  }

  @Operation(
    summary = "Workflow information list",
    description = "Get a list of all workflows of the workspace or a specific project, with information about how they can be executed via REST APIs.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(WorkflowApiDoc.workflowInfoListExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project has not been found."
      )
  ))
  @Parameter(
    name = "projectId",
    description = "If set then only workflows from this particular project are returned.",
    required = false,
    in = ParameterIn.QUERY,
    schema = new Schema(implementation = classOf[String])
  )
  def workflowInfoList(): Action[AnyContent] = RequestUserContextAction { request => implicit userContext =>
    val projects = request.queryString.get("projectId").toSeq.flatten.headOption match {
      case Some(projectId) => Seq(getProject(projectId))
      case None => allProjects
    }
    val workflowInfos = for(project <- projects;
        workflow <- project.tasks[Workflow]) yield {
      WorkflowInfo.fromWorkflow(workflow, project)
    }
    Ok(Json.toJson(workflowInfos))
  }

  @Operation(
    summary = "Workflow information",
    description = "Get information about how a specific workflow can be executed via REST APIs.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(WorkflowApiDoc.workflowInfoExample))
          ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      )
    ))
  def workflowInfo(@Parameter(
                     name = "projectId",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectId: String,
                   @Parameter(
                     name = "workflowId",
                     description = "The workflow identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   workflowId: String): Action[AnyContent] = RequestUserContextAction { request =>implicit userContext =>
    val (project, workflow) = projectAndTask[Workflow](projectId, workflowId)
    Ok(Json.toJson(WorkflowInfo.fromWorkflow(workflow, project)))
  }

  /** Returns a list of potential tasks that can be used in the workflow with their port configuration.
    *
    **/
  @Operation(
    summary = "Workflow nodes port config",
    description = "Returns information about what port configuration nodes in a workflow can have.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = WorkflowApiDoc.portsResponseDescription,
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(WorkflowApiDoc.portsResponseExample))
          ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or workflow has not been found."
      )
    ))
  def workflowNodesConfig(@Parameter(
                            name = "projectId",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectId: String,
                          @Parameter(
                            name = "workflowId",
                            description = "The workflow identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          workflowId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val (project, _) = projectAndTask[Workflow](projectId, workflowId)
    val customTaskPortConfigs: Seq[(String, WorkflowNodePortConfig)] = for(customTask <- project.tasks[CustomTask]) yield {
      val taskId = customTask.id.toString
      val portConfig: WorkflowNodePortConfig = customTask.data.inputSchemataOpt match {
        case Some(inputSchema) => WorkflowNodePortConfig(inputSchema.size)
        case None => WorkflowNodePortConfig(1, None)
      }
      (taskId, portConfig)
    }
    val workflowNodesPortConfig = WorkflowNodesPortConfig(
      byItemType = Map(
        ItemType.dataset.id -> WorkflowNodePortConfig(1, None),
        ItemType.workflow.id -> WorkflowNodePortConfig(1, None),
        ItemType.linking.id -> WorkflowNodePortConfig(2),
        ItemType.transform.id -> WorkflowNodePortConfig(1)
      ),
      byTaskId = customTaskPortConfigs.toMap,
      // FIXME CMEM-3457: Add workflow node specific port config and use this in the UI
      byNodeId = Map.empty
    )
    Ok(Json.toJson(workflowNodesPortConfig))
  }

  case class WorkflowFailedException(msg: String, ex: Option[Throwable] = None) extends RequestException(msg, ex) {

    override val errorTitle: String = "Workflow failed"

    override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_INTERNAL_ERROR)

  }
}
