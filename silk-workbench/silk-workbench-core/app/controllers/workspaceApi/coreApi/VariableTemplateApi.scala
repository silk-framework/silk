package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.coreApi.doc.VariableTemplateApiDoc
import controllers.workspaceApi.coreApi.variableTemplate.{ValidateVariableTemplateRequest, VariableTemplateValidationError, VariableTemplateValidationResponse}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.runtime.templating.GlobalTemplateVariables
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, InjectedController}

import javax.inject.Inject
import scala.util.control.NonFatal

/** Everything related to variable templates. */
@Tag(name = "Variable Templates", description = "Provides endpoints for variable template handling.")
class VariableTemplateApi @Inject()() extends InjectedController with UserContextActions with ControllerUtilsTrait {
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
            examples = Array(new ExampleObject("TODO")) // TODO
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
      val resultOrError: Either[String, String] = try {
        Left(GlobalTemplateVariables.resolveTemplateValue(validationRequest.templateString))
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
}