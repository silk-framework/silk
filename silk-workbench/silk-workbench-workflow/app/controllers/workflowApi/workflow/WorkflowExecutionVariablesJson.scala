package controllers.workflowApi.workflow

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.TaskSpec
import org.silkframework.serialization.json.TemplateVariableJson
import org.silkframework.workspace.activity.workflow.WorkflowExecutionVariables.ExecutionVariableRequirement
import org.silkframework.workspace.{ProjectTask, WorkbenchLinks}
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

  def fromRequirements(requirements: Seq[ExecutionVariableRequirement]): WorkflowExecutionVariablesJson = {
    WorkflowExecutionVariablesJson(requirements.map(WorkflowExecutionVariableJson.fromRequirement))
  }
}

@Schema(description = "A single execution variable of a workflow run.")
case class WorkflowExecutionVariableJson(@Schema(description = "The variable name, addressed as 'execution.<name>' in templates.", requiredMode = RequiredMode.REQUIRED)
                                         name: String,
                                         @Schema(description = "True, if the variable is referenced at execution time, but neither defined on the workflow nor set during the run. It has to be provided when the run is started.", requiredMode = RequiredMode.REQUIRED)
                                         required: Boolean,
                                         @Schema(description = "The default defined on the workflow itself. Values and templates of sensitive variables are omitted.", requiredMode = RequiredMode.NOT_REQUIRED, implementation = classOf[TemplateVariableJson])
                                         default: Option[TemplateVariableJson],
                                         @ArraySchema(
                                           schema = new Schema(
                                             description = "Sub-tasks that define a default of that name. Those defaults do not apply to the workflow run.",
                                             requiredMode = RequiredMode.REQUIRED,
                                             implementation = classOf[TaskReferenceJson]
                                           ))
                                         definedOn: Seq[TaskReferenceJson],
                                         @ArraySchema(
                                           schema = new Schema(
                                             description = "Tasks whose templates reference the variable at execution time.",
                                             requiredMode = RequiredMode.REQUIRED,
                                             implementation = classOf[TaskReferenceJson]
                                           ))
                                         referencedBy: Seq[TaskReferenceJson],
                                         @Schema(description = "True, if a 'Set execution variable' operator or transformer in the workflow sets the variable during the run.", requiredMode = RequiredMode.REQUIRED)
                                         setDuringExecution: Boolean,
                                         @ArraySchema(
                                           schema = new Schema(
                                             description = "Tasks that set the variable during the run.",
                                             requiredMode = RequiredMode.REQUIRED,
                                             implementation = classOf[TaskReferenceJson]
                                           ))
                                         setBy: Seq[TaskReferenceJson])

object WorkflowExecutionVariableJson {

  implicit val workflowExecutionVariableFormat: Format[WorkflowExecutionVariableJson] = Json.format[WorkflowExecutionVariableJson]

  def fromRequirement(requirement: ExecutionVariableRequirement): WorkflowExecutionVariableJson = {
    WorkflowExecutionVariableJson(
      name = requirement.name,
      required = requirement.required,
      default = requirement.default.map(TemplateVariableJson.masked),
      definedOn = requirement.definedOn.map(TaskReferenceJson.fromTask),
      referencedBy = requirement.referencedBy.map(TaskReferenceJson.fromTask),
      setDuringExecution = requirement.setDuringExecution,
      setBy = requirement.setBy.map(TaskReferenceJson.fromTask)
    )
  }
}

@Schema(description = "A reference to a task.")
case class TaskReferenceJson(@Schema(description = "The task identifier.", requiredMode = RequiredMode.REQUIRED)
                             id: String,
                             @Schema(description = "The task label.", requiredMode = RequiredMode.NOT_REQUIRED)
                             label: Option[String],
                             @Schema(description = "The task type, e.g., 'dataset', 'transform', 'linking', 'workflow' or 'task'.", requiredMode = RequiredMode.REQUIRED)
                             taskType: String)

object TaskReferenceJson {

  implicit val taskReferenceFormat: Format[TaskReferenceJson] = Json.format[TaskReferenceJson]

  def fromTask(task: ProjectTask[_ <: TaskSpec]): TaskReferenceJson = {
    TaskReferenceJson(task.id, task.metaData.label, WorkbenchLinks.taskType(task))
  }
}
