package controllers.workspaceApi

import controllers.core.RequestUserContextAction
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.validation.{SourcePathValidationRequest, SourcePathValidationResponse}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.UserContext
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, InjectedController}

import javax.inject.Inject

/** API to validate different aspects of workspace artifacts. */
class ValidationApi @Inject() () extends InjectedController with ControllerUtilsTrait {
  /** Validates the syntax of a Silk source path expression and returns parse error details.
    * Also validate prefix names that they have a valid prefix. */
  def validateSourcePath(projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext: UserContext =>
    implicit val prefixes: Prefixes = getProject(projectId).config.prefixes
    validateJson[SourcePathValidationRequest] { request =>
      val parseError = UntypedPath.partialParse(request.pathExpression).error
      val response = SourcePathValidationResponse(valid = parseError.isEmpty, parseError = parseError)
      Ok(Json.toJson(response))
    }
  }
}
