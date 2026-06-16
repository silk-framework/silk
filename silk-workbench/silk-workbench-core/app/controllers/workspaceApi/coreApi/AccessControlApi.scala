package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.workspace.access.AccessControlConfig
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Inject

@Tag(name = "Workbench")
class AccessControlApi @Inject()(cc: ControllerComponents) extends AbstractController(cc) with UserContextActions {

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

  @Operation(
    summary = "Access control groups",
    description = "Retrieves the access control groups known to the system. This might not be a complete list of all groups.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The access control configuration.",
        content =  Array(new Content(
          mediaType = "application/json",
          array = new ArraySchema(schema = new Schema(implementation = classOf[String]))
        ))
      )
    ))
  def accessControlGroups: Action[AnyContent] = UserContextAction { implicit userContext =>
    val groups = AccessControlGroupProvider().groups
    Ok(Json.toJson(groups))
  }
}