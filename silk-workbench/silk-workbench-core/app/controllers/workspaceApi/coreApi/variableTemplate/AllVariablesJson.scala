package controllers.workspaceApi.coreApi.variableTemplate

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.TemplateVariablesEvaluationException
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariablesManager, VariableScope}
import org.silkframework.serialization.json.{TemplateVariableErrorJson, TemplateVariableJson, TemplateVariablesJson}
import org.silkframework.workspace.{Project, WorkbenchLinks, WorkspaceFactory}
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
    * Collects the variables of the requested scopes across all projects the user has access to.
    * Sensitive variables are masked.
    */
  def collect(scopes: Set[VariableScope])(implicit userContext: UserContext): AllVariablesJson = {
    val global =
      if (scopes.contains(VariableScope.global)) {
        Some(TemplateVariablesJson(GlobalTemplateVariables.all.variables.map(TemplateVariableJson.masked)))
      } else {
        None
      }
    val projects = WorkspaceFactory().workspace.userProjects.map(ProjectVariablesJson.collect(_, scopes))
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
        val tasks = project.allTasks.map { task =>
          val (variables, errors) = ResolvedVariablesJson(task.executionVariablesValueHolder, masked = true)
          TaskVariablesJson(task.id, task.metaData.label, WorkbenchLinks.taskType(task), variables, Some(errors).filter(_.nonEmpty))
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
  * Converts the variables of one manager to JSON.
  * Templates are resolved against the non-sensitive parent variables. If the evaluation fails,
  * the stored values are kept and the issues are returned as errors.
  */
object ResolvedVariablesJson {

  def apply(manager: TemplateVariablesManager, masked: Boolean)
           (implicit userContext: UserContext): (Seq[TemplateVariableJson], Seq[TemplateVariableErrorJson]) = {
    val toJson: TemplateVariable => TemplateVariableJson =
      if (masked) TemplateVariableJson.masked else variable => TemplateVariableJson(variable)
    val allVariables = manager.all
    try {
      (allVariables.resolved(manager.parentVariables.withoutSensitiveVariables()).variables.map(toJson), Seq.empty)
    } catch {
      case ex: TemplateVariablesEvaluationException =>
        (allVariables.variables.map(toJson),
          ex.issues.map(issue => TemplateVariableErrorJson(issue.variable.name, issue.ex.getMessage)))
    }
  }
}
