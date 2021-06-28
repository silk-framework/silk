package controllers.workflowApi

import akka.util.ByteString
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.ProjectUtils.getProjectAndTask
import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils
import controllers.workflowApi.workflow.{WorkflowInfo, WorkflowNodePortConfig, WorkflowNodesPortConfig}
import controllers.workspaceApi.search.ItemType
import org.silkframework.config.CustomTask
import org.silkframework.workbench.workflow.WorkflowWithPayloadExecutor
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.http.HttpEntity
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject

/**
  * Workflow API.
  */
class ApiWorkflowApi @Inject()() extends InjectedController with ControllerUtilsTrait with UserContextActions {
  /**
    * Run a variable workflow, where some of the tasks are configured at request time and dataset payload may be
    * delivered inside the request.
    */
  def variableWorkflowResult(projectName: String,
                             workflowTaskName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, workflowTask) = getProjectAndTask[Workflow](projectName, workflowTaskName)

    val (workflowConfig, mimeTypeOpt) = VariableWorkflowRequestUtils.queryStringToWorkflowConfig(project, workflowTask)
    val activity = workflowTask.activity[WorkflowWithPayloadExecutor]
    val id = activity.startBlocking(workflowConfig)
    if(mimeTypeOpt.isDefined) {
      val outputResource = activity.instance(id).value().resourceManager.get(VariableWorkflowRequestUtils.OUTPUT_FILE_RESOURCE_NAME, mustExist = true)
      Result(
        header = ResponseHeader(OK, Map.empty),
        body = HttpEntity.Strict(ByteString(outputResource.loadAsBytes), mimeTypeOpt)
      )
    } else {
      NoContent
    }
  }

  /** Get all workflows of the workspace or a specific project with information about how these can be executed via API. */
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

  /** Get information about how the workflow can be executed via API. */
  def workflowInfo(projectId: String, workflowId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    val (project, workflow) = projectAndTask[Workflow](projectId, workflowId)
    Ok(Json.toJson(WorkflowInfo.fromWorkflow(workflow, project)))
  }

  /** Returns a list of potential tasks that can be used in the workflow with their port configuration.
    *
    **/
  def workflowNodesConfig(projectId: String, workflowId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
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
        ItemType.transform.id -> WorkflowNodePortConfig(1, None)
      ),
      byTaskId = customTaskPortConfigs.toMap,
      // FIXME CMEM-3457: Add workflow node specific port config and use this in the UI
      byNodeId = Map.empty
    )
    Ok(Json.toJson(workflowNodesPortConfig))
  }
}
