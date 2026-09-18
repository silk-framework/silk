package controllers.workflowApi.workflow

import controllers.workspaceApi.coreApi.variableTemplate.{ResolvedVariablesJson, TaskReferenceJson}
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.serialization.json.TemplateVariableJson
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.workflow.WorkflowExecutionVariables.ExecutionVariableRequirement
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{Format, Json}

@Schema(description = "The execution variables that a workflow run needs.")
case class WorkflowExecutionVariablesJson(@ArraySchema(
                                            schema = new Schema(
                                              description = "All execution variables that are referenced, set during the run or defined on the workflow, sorted by name.",
                                              requiredMode = RequiredMode.REQUIRED,
                                              implementation = classOf[WorkflowExecutionVariableJson]
                                            ))
                                          variables: Seq[WorkflowExecutionVariableJson])

object WorkflowExecutionVariablesJson {

  implicit val workflowExecutionVariablesFormat: Format[WorkflowExecutionVariablesJson] = Json.format[WorkflowExecutionVariablesJson]

  def fromRequirements(requirements: Seq[ExecutionVariableRequirement], workflowTask: ProjectTask[Workflow])
                      (implicit userContext: UserContext): WorkflowExecutionVariablesJson = {
    // The defaults are resolved and masked like all variables of the allVariables endpoint, so stored values never leak
    val (defaults, _) = ResolvedVariablesJson(workflowTask.executionVariablesValueHolder, masked = true)
    val defaultsByName = defaults.map(default => default.name -> default).toMap
    WorkflowExecutionVariablesJson(requirements.map(requirement => WorkflowExecutionVariableJson.fromRequirement(requirement, defaultsByName.get(requirement.name))))
  }
}

@Schema(description = "A single execution variable of a workflow run.")
case class WorkflowExecutionVariableJson(@Schema(description = "The variable name, addressed as 'execution.<name>' in templates.", requiredMode = RequiredMode.REQUIRED)
                                         name: String,
                                         @Schema(description = "True, if the variable is referenced at execution time, but neither defined on the workflow nor set by a preceding node of every referencing node. It has to be provided when the run is started.", requiredMode = RequiredMode.REQUIRED)
                                         required: Boolean,
                                         @Schema(description = "The default defined on the workflow itself. Values and templates of sensitive variables are omitted, as is the value of a default whose template fails to evaluate.", requiredMode = RequiredMode.NOT_REQUIRED, implementation = classOf[TemplateVariableJson])
                                         default: Option[TemplateVariableJson],
                                         @ArraySchema(
                                           schema = new Schema(
                                             description = "Tasks whose templates reference the variable at execution time.",
                                             requiredMode = RequiredMode.REQUIRED,
                                             implementation = classOf[TaskReferenceJson]
                                           ))
                                         referencedBy: Seq[TaskReferenceJson],
                                         @ArraySchema(
                                           schema = new Schema(
                                             description = "Tasks that set the variable during the run, wherever they are placed. A variable that is set after or beside every referencing node is still required.",
                                             requiredMode = RequiredMode.REQUIRED,
                                             implementation = classOf[TaskReferenceJson]
                                           ))
                                         setBy: Seq[TaskReferenceJson])

object WorkflowExecutionVariableJson {

  implicit val workflowExecutionVariableFormat: Format[WorkflowExecutionVariableJson] = Json.format[WorkflowExecutionVariableJson]

  def fromRequirement(requirement: ExecutionVariableRequirement, default: Option[TemplateVariableJson]): WorkflowExecutionVariableJson = {
    WorkflowExecutionVariableJson(
      name = requirement.name,
      required = requirement.required,
      default = default,
      referencedBy = requirement.referencedBy.map(TaskReferenceJson.fromTask),
      setBy = requirement.setBy.map(TaskReferenceJson.fromTask)
    )
  }
}
