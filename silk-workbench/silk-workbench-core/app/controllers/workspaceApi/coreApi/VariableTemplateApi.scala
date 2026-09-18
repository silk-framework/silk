package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.TaskLink
import controllers.workspaceApi.coreApi.VariableTemplateApi.VariableDependencies
import controllers.workspaceApi.coreApi.doc.VariableTemplateApiDoc
import controllers.workspaceApi.coreApi.variableTemplate.{AllVariablesJson, AutoCompleteVariableTemplateRequest, ResolvedVariablesJson, ValidateVariableTemplateRequest}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.operations.{DeleteVariableModification, UpdateVariableModification, UpdateVariablesModification}
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.{JsonHelpers, TemplateVariableJson, TemplateVariablesJson}
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.collection.immutable.ArraySeq
import scala.collection.mutable

/** Everything related to variable templates. */
@Tag(name = "Variable Templates", description = "Provides endpoints for variable template handling.")
class VariableTemplateApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Retrieve variables",
    description = "Retrieves all variables at a specific scope.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The variables.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(
            implementation = classOf[TemplateVariablesJson]
          )
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    )
  )
  def getVariables(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "task",
                     description = "The task identifier. If provided, retrieves the execution variables of that task instead of project variables.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   task: Option[String],
                   @Parameter(
                     name = "transitive",
                     description = "If true and the task is a workflow, returns all variables that have to be set when running the workflow: in addition to the workflow's own execution variables, the execution variables defined on every task that may take part in the execution (its operators and datasets, the tasks those tasks reference, and sub-workflows, recursively). Since only the executed workflow's variables seed a run, variables defined on sub-tasks have to be defined on the workflow or provided as overrides when starting the run. If the same variable is defined on multiple levels, the variable of the enclosing workflow is returned, matching the value that applies when the workflow is executed. Requires the 'task' parameter.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[Boolean])
                   )
                   transitive: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    if (transitive && task.isEmpty) {
      throw new BadUserInputException("The 'transitive' parameter can only be used together with the 'task' parameter.")
    }
    var (variables, errors) = ResolvedVariablesJson(project.variablesManager(task), masked = false)
    if (transitive) {
      val subTasks = project.anyTask(task.get).data match {
        case workflow: Workflow => workflow.subTasksRecursive(project)
        case _ => Seq.empty
      }
      // A variable of the enclosing workflow shadows sub-task variables of the same name.
      val seenNames = mutable.Set.from(variables.map(_.name))
      for (subTask <- subTasks) {
        val (subVariables, subErrors) = ResolvedVariablesJson(subTask.executionVariablesValueHolder, masked = false)
        val newVariables = subVariables.filterNot(variable => seenNames.contains(variable.name))
        seenNames ++= newVariables.map(_.name)
        variables ++= newVariables
        errors ++= subErrors.filter(error => newVariables.exists(_.name == error.variableName))
      }
    }
    val variablesJson = TemplateVariablesJson(variables, Some(errors).filter(_.nonEmpty))
    Ok(Json.toJson(variablesJson))
  }

  @Operation(
    summary = "Retrieve all variables",
    description = "Retrieves the global variables, the variables of all projects the user has access to and the execution variables of all their tasks in one request. Values and templates of sensitive variables are omitted.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The variables grouped by project and task.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(
            implementation = classOf[AllVariablesJson]
          )
        ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If an unknown scope has been requested."
      )
    )
  )
  def allVariables(@Parameter(
                     name = "scope",
                     description = "Comma-separated list of the scopes to include: 'global', 'project' and/or 'execution'. Defaults to all scopes. The sections of scopes that are not requested are omitted from the response.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   scope: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val scopeNames = scope.map(_.split(',').toSeq.map(_.trim).filter(_.nonEmpty)).filter(_.nonEmpty)
    val scopes = scopeNames match {
      case Some(names) =>
        names.map { name =>
          VariableScope.all.find(_.toString == name).getOrElse(
            throw new BadUserInputException(s"Unknown variable scope '$name'. Supported scopes: ${VariableScope.all.mkString(", ")}"))
        }.toSet
      case None =>
        VariableScope.all.toSet
    }
    Ok(Json.toJson(AllVariablesJson.collect(scopes)))
  }

  @Operation(
    summary = "Put variables",
    description = "Updates all variables at a specific scope.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the update has been successful."
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[TemplateVariablesJson]),
      )
    )
  )
  def putVariables(@Parameter(
                     name = "project",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectName: String,
                   @Parameter(
                     name = "task",
                     description = "The task identifier. If provided, updates the execution variables of that task instead of project variables.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   task: Option[String]): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val variables = JsonHelpers.fromJsonValidated[TemplateVariablesJson](request.body).convert
    UpdateVariablesModification(project, variables, task).execute()
    Ok
  }

  @Operation(
    summary = "Get variable",
    description = "Retrieves a single variable by name.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Requested variable",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(
            implementation = classOf[TemplateVariableJson]
          )
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or variable does not exist."
      )
    )
  )
  def getVariable(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "name",
                    description = "The variable name",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  variableName: String,
                  @Parameter(
                    name = "task",
                    description = "The task identifier. If provided, retrieves an execution variable of that task instead of a project variable.",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      Ok(Json.toJson(TemplateVariableJson(project.variablesManager(task).get(variableName))))
  }

  @Operation(
    summary = "Put variable",
    description = "Adds or updates a single variable.",
    responses = Array(
      new ApiResponse(
        responseCode = "200"
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[TemplateVariableJson]),
      )
    )
  )
  def putVariable(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "name",
                    description = "The variable name",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  variableName: String,
                  @Parameter(
                    name = "task",
                    description = "The task identifier. If provided, adds or updates an execution variable of that task instead of a project variable.",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  task: Option[String]): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val variable = Json.fromJson[TemplateVariableJson](request.body).get.convert
    if(variable.name != variableName) {
      throw new BadUserInputException(s"Variable name provided in the URL ($variableName) does not match variable name in the request body (${variable.name})")
    }
    UpdateVariableModification(project, variable, task).execute()
    Ok
  }

  @Operation(
    summary = "Remove variable",
    description = "Removes a single variable by name.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the variable has been removed successfully",
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the variable could not be removed because another variable depends on it.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(VariableTemplateApiDoc.cannotDeleteUsedVariableResponse))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or variable does not exist."
      )
    )
  )
  def deleteVariable(@Parameter(
                       name = "project",
                       description = "The project identifier",
                       required = true,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     projectName: String,
                     @Parameter(
                       name = "name",
                       description = "The variable name",
                       required = true,
                       in = ParameterIn.PATH,
                       schema = new Schema(implementation = classOf[String])
                     )
                     variableName: String,
                     @Parameter(
                       name = "task",
                       description = "The task identifier. If provided, deletes an execution variable of that task instead of a project variable.",
                       required = false,
                       in = ParameterIn.QUERY,
                       schema = new Schema(implementation = classOf[String])
                     )
                     task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    DeleteVariableModification(project, variableName, task).execute()
    Ok
  }

  @Operation(
    summary = "Variable dependencies",
    description = "Returns a list of variables and tasks that a to-be-removed variable depends on.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The dependencies",
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project or variable does not exist."
      )
    )
  )
  def variableDependencies(@Parameter(
                             name = "project",
                             description = "The project identifier",
                             required = true,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectName: String,
                           @Parameter(
                             name = "name",
                             description = "The variable name",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           variableName: String,
                           @Parameter(
                             name = "task",
                             description = "The task identifier. If provided, checks dependencies for an execution variable of that task instead of a project variable.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val modification = DeleteVariableModification(project, variableName, task)
    val dependentVariables = modification.dependentVariables()
    val dependentTaskLinks = modification.invalidTasks().map(task => TaskLink.fromTask(task))
    Ok(Json.toJson(VariableDependencies(dependentVariables, dependentTaskLinks)))
  }

  @Operation(
    summary = "Reorder variables",
    description = "Reorders all variables.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "If the update has been successful."
      ),
      new ApiResponse(
        responseCode = "400",
        description = "If the variables could not be reordered.",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the project does not exist."
      )
    )
  )
  @RequestBody(
    description = "An array containing the variable names in the desired order.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[Array[String]]),
        examples = Array(new ExampleObject("""[ "variable1", "variable2" ]"""))
      )
    )
  )
  def reorderVariables(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier. If provided, reorders the execution variables of that task instead of project variables.",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String])
                      )
                      task: Option[String]): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      val variableNames = ArraySeq.unsafeWrapArray(Json.fromJson[Array[String]](request.body).get)
      val manager = project.variablesManager(task)
      val currentVariables = manager.all

      if(currentVariables.map.keySet != variableNames.toSet) {
        throw new BadUserInputException("Provided variable names don't match the existing variables.")
      }

      val newVariables =
        for(variableName <- variableNames) yield {
           currentVariables.map(variableName)
        }

      val scope = task.map(_ => VariableScope.execution).getOrElse(VariableScope.project)
      val resolved = resolveWithDependencyCheck(TemplateVariables(newVariables), manager.parentVariables.withoutSensitiveVariables(), scope)
      task match {
        case Some(taskId) =>
          project.anyTask(taskId).updateExecutionVariables(resolved)
        case None =>
          project.templateVariables.put(resolved)
      }
      Ok
  }

  @Operation(
    summary = "Validate variable template",
    description = "Validate a template based on Jinja syntax that may contain global variables.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
            examples = Array(new ExampleObject(VariableTemplateApiDoc.validateVariableTemplateResponse))
          )
        )
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(VariableTemplateApiDoc.validateVariableTemplateRequest))
      )
    )
  )
  def validateTemplate():  Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ValidateVariableTemplateRequest] { validationRequest =>
      Ok(Json.toJson(validationRequest.execute()))
    }
  }

  @Operation(
    summary = "Auto-complete variable template",
    description = "Returns auto=completion suggestions for the variable template.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(`type` = "object"),
            examples = Array(new ExampleObject(VariableTemplateApiDoc.autoCompleteVariableTemplateResponse))
          )
        )
      )
    )
  )
  @RequestBody(
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(`type` = "object"),
        examples = Array(new ExampleObject(VariableTemplateApiDoc.autoCompleteVariableTemplateRequest))
      )
    )
  )
  def autoCompleteTemplate(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit uc =>
    validateJson[AutoCompleteVariableTemplateRequest] { autoCompleteRequest =>
      val response = autoCompleteRequest.execute()
      Ok(Json.toJson(response))
    }
  }

  /**
   * Resolves variables with dependency order checking.
   * If a variable references a variable of the same scope that is defined after it,
   * a CannotReorderVariablesException is thrown. Failures unrelated to the ordering
   * (e.g. templates referencing sensitive parent variables, which are not available here)
   * keep the variable's stored value instead. A reference to a sensitive sibling is rejected.
   */
  private def resolveWithDependencyCheck(variables: TemplateVariables, parentVars: TemplateVariables, scope: VariableScope): TemplateVariables = {
    val resolvedVariables = mutable.Buffer[TemplateVariable]()
    val dependencyErrors = mutable.LinkedHashMap[String, Seq[String]]()
    for (variable <- variables.variables) {
      variable.template match {
        case Some(template) =>
          try {
            resolvedVariables.append(variable.copy(value = variables.resolveTemplate(variable, template, parentVars, resolvedVariables.toSeq)))
          } catch {
            case ex: SensitiveVariableReferenceException =>
              throw ex // Never tolerated, the stored value would keep the sensitive value
            case unbound: UnboundVariablesException =>
              val missingSiblings = unbound.missingVars.filter(_.scope == scope)
              if (missingSiblings.nonEmpty) {
                dependencyErrors.put(variable.name, missingSiblings.map(_.name))
              }
              resolvedVariables.append(variable) // Keep the stored value
            case _: TemplateEvaluationException =>
              resolvedVariables.append(variable) // Keep the stored value
          }
        case None =>
          resolvedVariables.append(variable)
      }
    }
    if (dependencyErrors.nonEmpty) {
      throw new CannotReorderVariablesException(dependencyErrors.toMap)
    }
    TemplateVariables(resolvedVariables.toSeq)
  }
}

object VariableTemplateApi {

  case class VariableDependencies(@ArraySchema(
                                    schema = new Schema(
                                      description = "List of dependent variables.",
                                      implementation = classOf[String]
                                  ))
                                  dependentVariables: Seq[String],
                                  @ArraySchema(
                                    schema = new Schema(
                                      description = "List of dependent tasks.",
                                      implementation = classOf[TaskLink]
                                    ))
                                  dependentTasks: Seq[TaskLink])

  implicit val variableDependenciesFormat: OFormat[VariableDependencies] = Json.format[VariableDependencies]
}
