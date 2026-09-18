package controllers.workspaceApi.coreApi.variableTemplate

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariables, TemplateVariablesManager, VariableScope}
import org.silkframework.serialization.json.{TemplateVariableErrorJson, TemplateVariableJson, TemplateVariablesJson}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{Json, OFormat}

@Schema(description = "The variables of all projects the user has access to, including the execution variables of their tasks.")
case class AllVariablesJson(@Schema(
                              description = "The global variables. Omitted if the 'global' scope has not been requested.",
                              requiredMode = RequiredMode.NOT_REQUIRED,
                              implementation = classOf[TemplateVariablesJson]
                            )
                            global: Option[TemplateVariablesJson],
                            @ArraySchema(
                              schema = new Schema(
                                description = "All projects the user has access to.",
                                requiredMode = RequiredMode.REQUIRED,
                                implementation = classOf[ProjectVariablesJson]
                              ))
                            projects: Seq[ProjectVariablesJson])

object AllVariablesJson {

  implicit val allVariablesFormat: OFormat[AllVariablesJson] = Json.format[AllVariablesJson]

  /**
    * Collects the variables of the requested scopes across all projects the user has access to, or of one project.
    * Sensitive variables are masked.
    */
  def collect(scopes: Set[VariableScope], projectId: Option[String] = None)(implicit userContext: UserContext): AllVariablesJson = {
    val global =
      if (scopes.contains(VariableScope.global)) {
        Some(TemplateVariablesJson(GlobalTemplateVariables.all.variables.map(TemplateVariableJson.masked)))
      } else {
        None
      }
    val workspace = WorkspaceFactory().workspace
    val projects = projectId.map(id => Seq(workspace.project(id))).getOrElse(workspace.userProjects).map(ProjectVariablesJson.collect(_, scopes))
    AllVariablesJson(global, projects)
  }
}

@Schema(description = "The variables of a project and the execution variables of its tasks.")
case class ProjectVariablesJson(@Schema(description = "The project identifier.", requiredMode = RequiredMode.REQUIRED)
                                id: String,
                                @Schema(description = "The project label.", requiredMode = RequiredMode.NOT_REQUIRED)
                                label: Option[String],
                                @ArraySchema(
                                  schema = new Schema(
                                    description = "The project variables. Omitted if the 'project' scope has not been requested.",
                                    requiredMode = RequiredMode.NOT_REQUIRED,
                                    implementation = classOf[TemplateVariableJson]
                                  ))
                                variables: Option[Seq[TemplateVariableJson]],
                                @ArraySchema(
                                  schema = new Schema(
                                    description = "Evaluation errors of the project variables. Omitted if there are none.",
                                    requiredMode = RequiredMode.NOT_REQUIRED,
                                    implementation = classOf[TemplateVariableErrorJson]
                                  ))
                                errors: Option[Seq[TemplateVariableErrorJson]],
                                @ArraySchema(
                                  schema = new Schema(
                                    description = "All tasks of the project. Omitted if the 'execution' scope has not been requested.",
                                    requiredMode = RequiredMode.NOT_REQUIRED,
                                    implementation = classOf[TaskVariablesJson]
                                  ))
                                tasks: Option[Seq[TaskVariablesJson]],
                                @ArraySchema(
                                  schema = new Schema(
                                    description = "Tasks that could not be loaded and whose execution variables are therefore unknown. Omitted if there are none.",
                                    requiredMode = RequiredMode.NOT_REQUIRED,
                                    implementation = classOf[TaskLoadingErrorJson]
                                  ))
                                loadingErrors: Option[Seq[TaskLoadingErrorJson]])

object ProjectVariablesJson {

  implicit val projectVariablesFormat: OFormat[ProjectVariablesJson] = Json.format[ProjectVariablesJson]

  def collect(project: Project, scopes: Set[VariableScope])(implicit userContext: UserContext): ProjectVariablesJson = {
    val (variables, errors) =
      if (scopes.contains(VariableScope.project)) {
        val (variables, errors) = ResolvedVariablesJson(project.templateVariables, masked = true)
        (Some(variables), Some(errors).filter(_.nonEmpty))
      } else {
        (None, None)
      }
    val (tasks, loadingErrors) =
      if (scopes.contains(VariableScope.execution)) {
        // The parents of all execution variables of the project, merged once instead of per task
        val parentVariables = (GlobalTemplateVariables.all merge project.templateVariables.all).withoutSensitiveVariables()
        val tasks = project.allTasks.map { task =>
          val (variables, errors) = ResolvedVariablesJson(task.executionVariables, parentVariables, masked = true)
          TaskVariablesJson.fromTask(task, variables, Some(errors).filter(_.nonEmpty))
        }
        val loadingErrors = project.loadingErrors.map { error =>
          TaskLoadingErrorJson(error.taskId, error.label, Option(error.throwable.getMessage).getOrElse(error.throwable.getClass.getSimpleName))
        }
        (Some(tasks), Some(loadingErrors).filter(_.nonEmpty))
      } else {
        (None, None)
      }
    ProjectVariablesJson(project.id, project.config.metaData.label, variables, errors, tasks, loadingErrors)
  }
}

@Schema(description = "The execution variables of a task.")
case class TaskVariablesJson(@Schema(description = "The task identifier.", requiredMode = RequiredMode.REQUIRED)
                             id: String,
                             @Schema(description = "The task label.", requiredMode = RequiredMode.NOT_REQUIRED)
                             label: Option[String],
                             @Schema(description = TaskReferenceJson.taskTypeDescription, requiredMode = RequiredMode.REQUIRED)
                             taskType: String,
                             @ArraySchema(
                               schema = new Schema(
                                 description = "The execution variables of the task.",
                                 requiredMode = RequiredMode.REQUIRED,
                                 implementation = classOf[TemplateVariableJson]
                               ))
                             variables: Seq[TemplateVariableJson],
                             @ArraySchema(
                               schema = new Schema(
                                 description = "Evaluation errors of the execution variables. Omitted if there are none.",
                                 requiredMode = RequiredMode.NOT_REQUIRED,
                                 implementation = classOf[TemplateVariableErrorJson]
                               ))
                             errors: Option[Seq[TemplateVariableErrorJson]])

object TaskVariablesJson {
  implicit val taskVariablesFormat: OFormat[TaskVariablesJson] = Json.format[TaskVariablesJson]

  def fromTask(task: ProjectTask[_ <: TaskSpec], variables: Seq[TemplateVariableJson], errors: Option[Seq[TemplateVariableErrorJson]]): TaskVariablesJson = {
    val reference = TaskReferenceJson.fromTask(task)
    TaskVariablesJson(reference.id, reference.label, reference.taskType, variables, errors)
  }
}

@Schema(description = "A task that could not be loaded.")
case class TaskLoadingErrorJson(@Schema(description = "The task identifier.", requiredMode = RequiredMode.REQUIRED)
                                id: String,
                                @Schema(description = "The task label.", requiredMode = RequiredMode.NOT_REQUIRED)
                                label: Option[String],
                                @Schema(description = "The loading error message.", requiredMode = RequiredMode.REQUIRED)
                                message: String)

object TaskLoadingErrorJson {
  implicit val taskLoadingErrorFormat: OFormat[TaskLoadingErrorJson] = Json.format[TaskLoadingErrorJson]
}

/**
  * Converts the variables of one scope to JSON.
  * Templates are resolved against the non-sensitive parent variables. If the evaluation fails,
  * the stored values are kept and the issues are returned as errors. When masking, the stored values of the
  * failed variables are omitted, since a value that its template can no longer produce may hold a sensitive value,
  * and the error message of a sensitive variable is replaced, since it may quote the template.
  */
object ResolvedVariablesJson {

  /** The error message reported for a sensitive variable when masking. */
  final val maskedErrorMessage = "The template of this sensitive variable could not be evaluated."

  def apply(manager: TemplateVariablesManager, masked: Boolean)
           (implicit userContext: UserContext): (Seq[TemplateVariableJson], Seq[TemplateVariableErrorJson]) = {
    apply(manager.all, manager.parentVariables.withoutSensitiveVariables(), masked)
  }

  /**
    * @param parentVariables The variables of the parent scopes, without sensitive ones.
    */
  def apply(variables: TemplateVariables, parentVariables: TemplateVariables, masked: Boolean): (Seq[TemplateVariableJson], Seq[TemplateVariableErrorJson]) = {
    val toJson: TemplateVariable => TemplateVariableJson = if (masked) TemplateVariableJson.masked else TemplateVariableJson(_)
    try {
      (variables.resolved(parentVariables).variables.map(toJson), Seq.empty)
    } catch {
      case ex: TemplateVariablesEvaluationException =>
        val failed = ex.issues.map(_.variable.name).toSet
        val variablesJson = variables.variables.map { variable =>
          val json = toJson(variable)
          if (masked && failed.contains(variable.name)) json.copy(value = None) else json
        }
        val errors = ex.issues.map { issue =>
          val message = if (masked && issue.variable.isSensitive) maskedErrorMessage else issue.ex.getMessage
          TemplateVariableErrorJson(issue.variable.name, message)
        }
        (variablesJson, errors)
    }
  }
}
