package controllers.workspaceApi

import controllers.autoCompletion.CompletionBase
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.WorkspaceApiDoc
import controllers.workspace.workspaceRequests.{VocabularyInfo, VocabularyInfos}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.rule.vocab.VocabularyProperty
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.StringUtils
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import scala.collection.mutable.ArrayBuffer

@Tag(name = "Workspace vocabularies")
class WorkspaceVocabularyApi extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Get all globally registered vocabularies",
    description = "Fetches all vocabularies that are registered in the global vocabulary cache.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All vocabularies in the cache.",
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

  @Operation(
    summary = "Find properties in the global vocabulary cache.",
    description = "Fetches a number of properties from the global vocabulary cache that match a text query.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "An array of properties that match the text query.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(WorkspaceApiDoc.globalVocabularyFindPropertyExample))
        ))
      )
    )
  )
  def findPropertyInGlobalVocabularyCache(@Parameter(
                                            name = "textQuery",
                                            description = "The search query. This can be a multi word text string. All words must be present.",
                                            required = true,
                                            in = ParameterIn.QUERY,
                                            schema = new Schema(implementation = classOf[String])
                                          )
                                          textQuery: String,
                                          @Parameter(
                                            name = "limit",
                                            description = "The max. number of results.",
                                            required = true,
                                            in = ParameterIn.QUERY,
                                            schema = new Schema(implementation = classOf[String])
                                          )
                                          limit: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val cache = workspace.activity[GlobalVocabularyCache]
    cache.value.get match {
      case Some(gvc) =>
        val matches = findProperties(gvc, textQuery, limit)
        val sorted = matches.sortBy(_.info.labelValue)
        val completions = sorted.map(p => {
          val label = p.info.labelValue
          val uri = p.info.uri
          CompletionBase(
            value = p.info.uri,
            label = if(label != uri) Some(label) else None
          )
        })
        Ok(Json.toJson(completions))
      case None =>
        throw NotFoundException("No value available (yet) for global vocabulary cache.")
    }
  }

  private def findProperties(gvc: VocabularyCacheValue,
                             textQuery: String,
                             limit: Int): Seq[VocabularyProperty] = {
    val results = ArrayBuffer[VocabularyProperty]()
    val searchWords = StringUtils.extractSearchTerms(textQuery).map(_.toLowerCase)
    for (vocabulary <- gvc.vocabularies;
         property <- vocabulary.properties) {
      val info = property.info
      val searchContent = s"${info.uri} ${info.labelValue}"
      if(StringUtils.matchesSearchTerm(searchWords, searchContent)) {
        results.append(property)
      }
      if (results.size >= limit) {
        return results
      }
    }
    results
  }
}
