package controllers.workspaceApi.coreApi

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.workspace.access.AccessControlConfig
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Inject

@Tag(name = "Workbench")
class AccessControlApi @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private implicit val accessControlConfigFormat: Format[AccessControlConfig] = Json.format[AccessControlConfig]

  @Operation(
    summary = "Access control configuration",
    description = "Retrieves the current access control configuration.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The access control configuration.",
        content = Array(new Content(
          mediaType = "application/json"
        ))
      )
    ))
  def accessControl: Action[AnyContent] = Action {
    Ok(Json.toJson(AccessControlConfig()))
  }

}