package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.validation.{SourcePathValidationRequest, SourcePathValidationResponse}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.UserContext
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, InjectedController}

import javax.inject.Inject

/** API to validate different aspects of workspace artifacts. */
@Tag(name = "Validation", description = "Validate paths.")
class ValidationApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {
  /** Validates the syntax of a Silk source path expression and returns parse error details.
    * Also validate prefix names that they have a valid prefix. */
  @Operation(
    summary = "Source path validation",
    description = "Validates the syntax of a provided path string.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[SourcePathValidationResponse]),
            examples = Array(new ExampleObject("{ \"valid\": false, \"parseError\": { \"offset\": 13, \"message\": \"[ expected but w found\", \"inputLeadingToError\": \" \" } }")))
        )
      )
    ))
  @RequestBody(
    description = "Request to validate the path syntax of the path string from the `pathExpression` parameter.",
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[SourcePathValidationRequest]),
        examples = Array(new ExampleObject("{ \"pathExpression\": \"/invalid/path with spaces at the wrong place\" }"))
      )
    )
  )
  def validateSourcePath(@Parameter(
                           name = "projectId",
                           description = "The project identifier",
                           required = true,
                           in = ParameterIn.PATH,
                           schema = new Schema(implementation = classOf[String])
                         )
                         projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext: UserContext =>
    implicit val prefixes: Prefixes = getProject(projectId).config.prefixes
    validateJson[SourcePathValidationRequest] { request =>
      val parseError = UntypedPath.partialParse(request.pathExpression).error
      val response = SourcePathValidationResponse(valid = parseError.isEmpty, parseError = parseError)
      Ok(Json.toJson(response))
    }
  }
}
