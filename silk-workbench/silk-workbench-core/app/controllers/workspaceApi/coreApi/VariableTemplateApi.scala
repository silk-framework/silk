package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.coreApi.VariableTemplateApi.TemplateVariablesFormat
import controllers.workspaceApi.coreApi.doc.VariableTemplateApiDoc
import controllers.workspaceApi.coreApi.variableTemplate.{AutoCompleteVariableTemplateRequest, ValidateVariableTemplateRequest, VariableTemplateValidationError, VariableTemplateValidationResponse}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariable, TemplateVariables}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.util.control.NonFatal

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
            implementation = classOf[TemplateVariablesFormat]
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
    Ok(Json.toJson(TemplateVariablesFormat(project.templateVariables.all)))
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
        schema = new Schema(implementation = classOf[TemplateVariablesFormat]),
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
    val variables = Json.fromJson[TemplateVariablesFormat](request.body).get.convert
    project.templateVariables.put(variables)
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
            examples = Array(new ExampleObject(VariableTemplateApiDoc.validateVariableTemplateResponse)) // TODO
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
  //TODO add project parameter
  def validateTemplate():  Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[ValidateVariableTemplateRequest] { validationRequest =>
      val resultOrError: Either[String, String] = try {
        Left(GlobalTemplateVariables.all.resolveTemplateValue(validationRequest.templateString))
      } catch {
        case NonFatal(ex) =>
          Right(ex.getMessage)
      }
      val response = VariableTemplateValidationResponse(
        valid = resultOrError.isLeft,
        parseError = resultOrError.right.toOption.map(errorMessage => VariableTemplateValidationError(
          message = errorMessage,
          start = 0,
          end = validationRequest.templateString.length
        )),
        evaluatedTemplate = resultOrError.left.toOption
      )
      Ok(Json.toJson(response))
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
  //TODO add project parameter
  def autoCompleteTemplate(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit uc =>
    validateJson[AutoCompleteVariableTemplateRequest] { autoCompleteRequest =>
      val response = autoCompleteRequest.execute()
      Ok(Json.toJson(response))
    }
  }
}

object VariableTemplateApi {

  @Schema(description = "A single template variable")
  case class TemplateVariableFormat(@Schema(
                                      description = "The name of the variable.",
                                      example = "myVar",
                                      required = true
                                    )
                                    name: String,
                                    @Schema(
                                      description = "The value of the variable.",
                                      example = "example value",
                                      required = false
                                    )
                                    value: Option[String],
                                    @Schema(
                                      description = "Template to generate the variable value.",
                                      required = false
                                    )
                                    template: Option[String],
                                    @Schema(
                                      description = "True, if this is a sensitive variable that should not be exposed to the user.",
                                      example = "false",
                                      required = true
                                    )
                                    isSensitive: Boolean,
                                    @Schema(
                                      description = "The scope of the variable.",
                                      example = "project",
                                      required = true
                                    )
                                    scope: String) {
    def convert: TemplateVariable = {
      if(value.isEmpty && template.isEmpty) {
        throw new BadUserInputException("Either the variable value or its template has to be defined.")
      }
      TemplateVariable(name, value.getOrElse(""), template, isSensitive, scope)
    }
  }

  object TemplateVariableFormat {
    def apply(variable: TemplateVariable): TemplateVariableFormat = {
      TemplateVariableFormat(variable.name, Some(variable.value), variable.template, variable.isSensitive, variable.scope)
    }
  }

  @Schema(description = "A list of template variables.")
  case class TemplateVariablesFormat(variables: Seq[TemplateVariableFormat]) {
    def convert: TemplateVariables = {
      TemplateVariables(variables.map(_.convert))
    }
  }

  object TemplateVariablesFormat {
    def apply(variables: TemplateVariables): TemplateVariablesFormat = {
      TemplateVariablesFormat(variables.variables.map(TemplateVariableFormat(_)))
    }
  }

  implicit val templateVariableFormat: OFormat[TemplateVariableFormat] = Json.format[TemplateVariableFormat]
  implicit val templateVariablesFormat: OFormat[TemplateVariablesFormat] = Json.format[TemplateVariablesFormat]
}