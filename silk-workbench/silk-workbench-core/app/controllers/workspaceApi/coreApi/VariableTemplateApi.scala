package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.util.TaskLink
import controllers.workspaceApi.coreApi.VariableTemplateApi.{TemplateVariableJson, TemplateVariablesJson, VariableDependencies}
import controllers.workspaceApi.coreApi.doc.VariableTemplateApiDoc
import controllers.workspaceApi.coreApi.variableTemplate.{AutoCompleteVariableTemplateRequest, ValidateVariableTemplateRequest}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.templating.exceptions._
import org.silkframework.runtime.templating.operations.{DeleteVariableModification, UpdateVariableModification, UpdateVariablesModification}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariables}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.WorkspaceFactory
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
                   projectName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val allVariables = project.templateVariables.all
    val variablesJson = {
      try {
        TemplateVariablesJson(allVariables.resolved())
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
                   projectName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val variables = Json.fromJson[TemplateVariablesJson](request.body).get.convert
    UpdateVariablesModification(project, variables).execute()
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
                  variableName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      Ok(Json.toJson(TemplateVariableJson(project.templateVariables.get(variableName))))
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
                  variableName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val variable = Json.fromJson[TemplateVariableJson](request.body).get.convert
    if(variable.name != variableName) {
      throw new BadUserInputException(s"Variable name provided in the URL ($variableName) does not match variable name in the request body (${variable.name})")
    }
    UpdateVariableModification(project, variable).execute()
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
                     variableName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    DeleteVariableModification(project, variableName).execute()
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
                           variableName: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val modification = DeleteVariableModification(project, variableName)
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
                      projectName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
      val project = WorkspaceFactory().workspace.project(projectName)
      val variableNames = ArraySeq.unsafeWrapArray(Json.fromJson[Array[String]](request.body).get)

      val currentVariables = project.templateVariables.all
      if(project.templateVariables.all.map.keySet != variableNames.toSet) {
        throw new BadUserInputException("Provided variable names don't match the existing variables.")
      }

      val newVariables =
        for(variableName <- variableNames) yield {
           currentVariables.map(variableName)
        }

      try {
        project.templateVariables.put(TemplateVariables(newVariables).resolved(GlobalTemplateVariables.all))
      } catch {
        case ex: TemplateVariablesEvaluationException =>
          val dependencyErrors =
            ex.issues.collect {
              case TemplateVariableEvaluationException(dependentVar, unboundEx: UnboundVariablesException) =>
                (dependentVar.name, unboundEx.missingVars.filter(_.scope == "project").map(_.name))
            }.filter(_._2.nonEmpty).toMap
          if(dependencyErrors.nonEmpty) {
            throw new CannotReorderVariablesException(dependencyErrors)
          } else {
            throw ex
          }
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
}

object VariableTemplateApi {

  @Schema(description = "A single template variable")
  case class TemplateVariableJson(@Schema(
                                      description = "The name of the variable.",
                                      example = "myVar",
                                      requiredMode = RequiredMode.REQUIRED
                                  )
                                  name: String,
                                  @Schema(
                                    description = "The value of the variable.",
                                    example = "example value",
                                    requiredMode = RequiredMode.NOT_REQUIRED
                                  )
                                  value: Option[String],
                                  @Schema(
                                    description = "Template to generate the variable value.",
                                    requiredMode = RequiredMode.NOT_REQUIRED
                                  )
                                  template: Option[String],
                                  @Schema(
                                    description = "Optional description for documentation.",
                                    example = "Example description",
                                    requiredMode = RequiredMode.NOT_REQUIRED
                                  )
                                  description: Option[String],
                                  @Schema(
                                    description = "True, if this is a sensitive variable that should not be exposed to the user.",
                                    example = "false",
                                    requiredMode = RequiredMode.REQUIRED
                                  )
                                  isSensitive: Boolean,
                                  @Schema(
                                    description = "The scope of the variable.",
                                    example = "project",
                                    requiredMode = RequiredMode.REQUIRED
                                  )
                                  scope: String) {
    def convert: TemplateVariable = {
      if (value.isEmpty && template.isEmpty) {
        throw new BadUserInputException("Either the variable value or its template has to be defined.")
      }
      TemplateVariable(name, value.getOrElse(""), template, description, isSensitive, scope)
    }
  }

  object TemplateVariableJson {
    def apply(variable: TemplateVariable): TemplateVariableJson = {
      TemplateVariableJson(variable.name, Some(variable.value), variable.template, variable.description, variable.isSensitive, variable.scope)
    }
  }

  @Schema(description = "A list of template variables.")
  case class TemplateVariablesJson(@ArraySchema(
                                       schema = new Schema(
                                        description = "List of variables.",
                                        requiredMode = RequiredMode.REQUIRED,
                                        implementation = classOf[TemplateVariableJson]
                                     ))
                                     variables: Seq[TemplateVariableJson],
                                   @ArraySchema(
                                       schema = new Schema(
                                         description = "List of evaluation errors.",
                                         requiredMode = RequiredMode.NOT_REQUIRED,
                                         implementation = classOf[TemplateVariableErrorJson]
                                     ))
                                     errors: Seq[TemplateVariableErrorJson] = Seq.empty) {
    def convert: TemplateVariables = {
      TemplateVariables(variables.map(_.convert))
    }
  }

  @Schema(description = "An error message related to a variable.")
  case class TemplateVariableErrorJson(variableName: String, message: String)


  object TemplateVariablesJson {
    def apply(variables: TemplateVariables): TemplateVariablesJson = {
      TemplateVariablesJson(variables.variables.map(TemplateVariableJson(_)))
    }

    def apply(variables: TemplateVariables, ex: TemplateVariablesEvaluationException): TemplateVariablesJson = {
      TemplateVariablesJson(variables.variables.map(TemplateVariableJson(_)), ex.issues.map(e => TemplateVariableErrorJson(e.variable.name, e.ex.getMessage)))
    }
  }

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

  implicit val templateVariableErrorFormat: OFormat[TemplateVariableErrorJson] = Json.format[TemplateVariableErrorJson]
  implicit val templateVariableFormat: OFormat[TemplateVariableJson] = Json.format[TemplateVariableJson]
  implicit val templateVariablesFormat: OFormat[TemplateVariablesJson] = Json.format[TemplateVariablesJson]
  implicit val variableDependenciesFormat: OFormat[VariableDependencies] = Json.format[VariableDependencies]
}