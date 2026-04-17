package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.TaskLink
import controllers.workspaceApi.coreApi.VariableTemplateApi.VariableDependencies
import controllers.workspaceApi.coreApi.doc.VariableTemplateApiDoc
import controllers.workspaceApi.coreApi.variableTemplate.{AutoCompleteVariableTemplateRequest, ValidateVariableTemplateRequest}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.operations.{DeleteVariableModification, UpdateVariableModification, UpdateVariablesModification}
import org.silkframework.runtime.templating.{TemplateVariableScopes, TemplateVariables, TemplateVariablesManager}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.{JsonHelpers, TemplateVariableJson, TemplateVariablesJson}
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.collection.immutable.ArraySeq

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
                     description = "The task identifier. If provided, retrieves task variables instead of project variables.",
                     required = false,
                     in = ParameterIn.QUERY,
                     schema = new Schema(implementation = classOf[String])
                   )
                   task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val manager = templateVariablesManager(project, task)
    val allVariables = manager.all
    val variablesJson = {
      try {
        TemplateVariablesJson(allVariables.resolved(manager.parentVariables))
      } catch {
        case ex: TemplateVariablesEvaluationException =>
          TemplateVariablesJson(allVariables, ex)
      }
    }
    Ok(Json.toJson(variablesJson))
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
                     description = "The task identifier. If provided, updates task variables instead of project variables.",
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
                    description = "The task identifier. If provided, retrieves a task variable instead of a project variable.",
                    required = false,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      Ok(Json.toJson(TemplateVariableJson(templateVariablesManager(project, task).get(variableName))))
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
                    description = "The task identifier. If provided, adds or updates a task variable instead of a project variable.",
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
                       description = "The task identifier. If provided, deletes a task variable instead of a project variable.",
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
                             description = "The task identifier. If provided, checks dependencies for a task variable instead of a project variable.",
                             required = false,
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String])
                           )
                           task: Option[String]): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    task match {
      case Some(_) =>
        // Task variables are only scoped to the task itself and have no cross-task dependencies
        Ok(Json.toJson(VariableDependencies(Seq.empty, Seq.empty)))
      case None =>
        val modification = DeleteVariableModification(project, variableName)
        val dependentVariables = modification.dependentVariables()
        val dependentTaskLinks = modification.invalidTasks().map(task => TaskLink.fromTask(task))
        Ok(Json.toJson(VariableDependencies(dependentVariables, dependentTaskLinks)))
    }
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
                        description = "The task identifier. If provided, reorders task variables instead of project variables.",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String])
                      )
                      task: Option[String]): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      val variableNames = ArraySeq.unsafeWrapArray(Json.fromJson[Array[String]](request.body).get)
      val manager = templateVariablesManager(project, task)
      val currentVariables = manager.all

      if(currentVariables.map.keySet != variableNames.toSet) {
        throw new BadUserInputException("Provided variable names don't match the existing variables.")
      }

      val newVariables =
        for(variableName <- variableNames) yield {
           currentVariables.map(variableName)
        }

      val scope = task.map(_ => TemplateVariableScopes.task).getOrElse(TemplateVariableScopes.project)
      val resolved = resolveWithDependencyCheck(TemplateVariables(newVariables), manager.parentVariables, scope)
      task match {
        case Some(taskId) =>
          project.anyTask(taskId).updateVariables(resolved)
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
   * Returns the TemplateVariablesManager for either the project or a specific task within the project.
   */
  private def templateVariablesManager(project: Project, task: Option[String])
                                      (implicit userContext: UserContext): TemplateVariablesManager = {
    task match {
      case Some(taskId) => project.anyTask(taskId).variablesValueHolder
      case None => project.templateVariables
    }
  }

  /**
   * Resolves variables with dependency order checking.
   * If the resolution fails because a variable references another variable that is defined after it,
   * a CannotReorderVariablesException is thrown.
   */
  private def resolveWithDependencyCheck(variables: TemplateVariables, parentVars: TemplateVariables, scope: Seq[String]): TemplateVariables = {
    try {
      variables.resolved(parentVars)
    } catch {
      case ex: TemplateVariablesEvaluationException =>
        val dependencyErrors =
          ex.issues.collect {
            case TemplateVariableEvaluationException(dependentVar, unboundEx: UnboundVariablesException) =>
              (dependentVar.name, unboundEx.missingVars.filter(_.scope == scope).map(_.name))
          }.filter(_._2.nonEmpty).toMap
        if(dependencyErrors.nonEmpty) {
          throw new CannotReorderVariablesException(dependencyErrors)
        } else {
          throw ex
        }
    }
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
