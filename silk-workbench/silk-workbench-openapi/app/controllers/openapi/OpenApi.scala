package controllers.openapi

import akka.util.ByteString
import io.aurora.utils.play.swagger.SwaggerPlugin
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.openapi.{OpenApiGenerator, OpenApiValidator, ValidationResult}
import org.silkframework.runtime.validation.BadUserInputException
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Results}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Tag(name = "OpenAPI", description = "Retrieve this OpenAPI specification.")
class OpenApi @Inject()(cc: ControllerComponents,
                        swaggerPlugin: SwaggerPlugin,
                                 )(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  private val AccessControlAllowOrigin: (String, String) = ("Access-Control-Allow-Origin", "*")

  @Operation(
    summary = "Generate OpenAPI JSON",
    description = "Generate OpenAPI specification as JSON",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def openApiJson: Action[AnyContent] = Action {
    val response = OpenApiGenerator.generateJson(swaggerPlugin)
    Results
      .Ok(ByteString(response))
      .as(ContentTypes.JSON)
      .withHeaders(AccessControlAllowOrigin)
  }

  @Operation(
    summary = "Generate OpenAPI YAML",
    description = "Generate OpenAPI specification as YAML",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "text/vnd.yaml"
        ))
      )
    ))
  def openApiYaml: Action[AnyContent] = Action {
    val response = OpenApiGenerator.generateYaml(swaggerPlugin)
    Results
      .Ok(ByteString(response))
      .as("text/vnd.yaml")
      .withHeaders(AccessControlAllowOrigin)
  }

  @Operation(
    summary = "Validate OpenAPI",
    description = "Validate OpenAPI specification",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[ValidationResult])
          ))
      ))
  )
  def validate(@Parameter(
    name = "url",
    description = "The URL of the OpenAPI specification",
    required = true,
    in = ParameterIn.QUERY,
    schema = new Schema(implementation = classOf[String])
  )
               url: Option[String]): Action[AnyContent] = Action { implicit request =>
    url match {
      case Some(url) =>
        val result = OpenApiValidator.validate(url)
        Ok(Json.toJson(result))
      case None =>
        throw new BadUserInputException("Parameter 'url' is missing.")
    }

  }

}
