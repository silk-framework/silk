package controllers.openapi

import akka.util.ByteString
import io.aurora.utils.play.swagger.SwaggerPlugin
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.openapi.{OpenApiGenerator, OpenApiValidator, ValidationResult}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@Tag(name = "OpenAPI", description = "Retrieve this OpenAPI specification.")
class OpenApi @Inject()(cc: ControllerComponents,
                        swaggerPlugin: SwaggerPlugin)
                       (implicit executionContext: ExecutionContext) extends AbstractController(cc) {

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
                 description = "The URL of the OpenAPI specification. Leave empty for validating the current OpenAPI spec.",
                 required = false,
                 in = ParameterIn.QUERY,
                 schema = new Schema(implementation = classOf[String])
               )
               url: Option[String]): Action[AnyContent] = Action { implicit request =>
    val result = OpenApiValidator.validate(swaggerPlugin, url)
    Ok(Json.toJson(result))
  }

}
