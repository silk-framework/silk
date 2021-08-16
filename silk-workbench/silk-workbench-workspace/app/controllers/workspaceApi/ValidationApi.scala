package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.validation.{AutoSuggestValidationError, AutoSuggestValidationResponse, SourcePathValidationRequest, UriPatternValidationRequest}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.util.{UriPatternParser, UriPatternValidationError}
import org.silkframework.rule.util.UriPatternParser.UriPatternParserException
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
            schema = new Schema(implementation = classOf[AutoSuggestValidationResponse]),
            examples = Array(new ExampleObject("{ \"valid\": false, \"parseError\": { \"start\": 13, \"end\": 15, \"message\": \"[ expected but w found\" } }")))
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
      val parseError = UntypedPath.partialParse(request.pathExpression).error.map { parseError =>
        val start = if(parseError.inputLeadingToError.length > 1) {
          parseError.offset - parseError.inputLeadingToError.length + 1
        } else {
          parseError.offset
        }
        val end = parseError.offset + 2;
        AutoSuggestValidationError(parseError.message, start, end)
      }
      val response = AutoSuggestValidationResponse(valid = parseError.isEmpty, parseError = parseError)
      Ok(Json.toJson(response))
    }
  }

  /** Validates the syntax of a URI pattern and checks if it would generate a valid URI.
    * Also validate prefix names that they have a valid prefix. */
  @Operation(
    summary = "URI pattern validation",
    description = "Validates the syntax of the provided URI pattern.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[AutoSuggestValidationResponse]),
            examples = Array(new ExampleObject("{ \"valid\": false, \"parseError\": { \"start\": 13, \"end\": 15, \"message\": \"[ expected but w found\" } }")))
        )
      )
    ))
  @RequestBody(
    description = "Request to validate the path syntax of the path string from the `pathExpression` parameter.",
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[UriPatternValidationRequest]),
        examples = Array(new ExampleObject("{ \"uriPattern\": \"urn:{path}/invalid path\" }"))
      )
    )
  )
  def validateUriPattern(
                          @Parameter(
                            name = "projectId",
                            description = "The project identifier",
                            required = true,
                            in = ParameterIn.PATH,
                            schema = new Schema(implementation = classOf[String])
                          )
                          projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext: UserContext =>
      implicit val prefixes: Prefixes = getProject(projectId).config.prefixes
      validateJson[UriPatternValidationRequest] { request =>
        val response: AutoSuggestValidationResponse = try {
          val validationResult = UriPatternParser.parseIntoSegments(request.uriPattern).validationResult()
          validationResult.validationError match {
            case Some(UriPatternValidationError(msg, (from, to))) =>
              AutoSuggestValidationResponse(valid = false, Some(AutoSuggestValidationError(msg, from, to)))
            case None =>
              AutoSuggestValidationResponse(valid = true, None)
          }
        } catch {
          case UriPatternParserException(msg, (start, end), _, _) =>
            AutoSuggestValidationResponse(valid = false, parseError = Some(AutoSuggestValidationError(msg, start, end)))
        }
        Ok(Json.toJson(response))
      }
  }
}
