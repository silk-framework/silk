package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.WorkspaceApiDoc
import controllers.workspace.workspaceRequests.{VocabularyInfo, VocabularyInfos}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

@Tag(name = "Workspace vocabularies")
class WorkspaceVocabularyApi extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Get all globally registered vocabularies",
    description = "Fetches all vocabularies that are registered in the global vocabulary cache.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(WorkspaceApiDoc.targetVocabularyExample))
        ))
      )
    )
  )
  def vocabularies(): Action[AnyContent] = UserContextAction { implicit userContext =>
    val vocabularies = VocabularyCacheValue.globalVocabularies
    val vocabInfoSeq = vocabularies map { vocab =>
      val label = vocab.info.label.orElse(vocab.info.altLabels.headOption)
      VocabularyInfo(vocab.info.uri, label, nrClasses = vocab.classes.size, nrProperties = vocab.properties.size)
    }
    Ok(Json.toJson(VocabularyInfos(vocabInfoSeq)))
  }
}
