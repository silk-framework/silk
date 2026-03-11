package controllers.workspaceApi.coreApi

import controllers.core.UserContextActions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workbench.utils.ErrorResult.ErrorResultFormat
import org.silkframework.workspace.access.AccessControlConfig
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Inject

@Tag(name = "Workbench")
class UserApi @Inject()(cc: ControllerComponents) extends AbstractController(cc) with UserContextActions {

  @Operation(
    summary = "User data",
    description = "Retrieves the data of the currently logged in user.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The user data.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[UserData])
        ))
      ),
      new ApiResponse(
        responseCode = "401",
        description = "If no user is logged in.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ErrorResultFormat])
        ))
      )
    ))
  def userData: Action[AnyContent] = UserContextAction { implicit userContext =>
    userContext.user match {
      case Some(user) =>
        val isAccessControlAdmin = user.actions.contains(AccessControlConfig().adminAction)
        Ok(Json.toJson(UserData(user.uri, user.label, user.groups.toSeq.sorted, isAccessControlAdmin)))
      case None =>
        ErrorResult(401, "Unauthorized", "No user logged in.")
    }
  }

}

case class UserData(uri: String,
                    label: String,
                    groups: Seq[String],
                    isAccessControlAdmin: Boolean)

object UserData {
  implicit val userDataFormat: Format[UserData] = Json.format[UserData]
}
