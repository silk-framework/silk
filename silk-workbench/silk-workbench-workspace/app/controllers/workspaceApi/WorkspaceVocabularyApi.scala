package controllers.workspaceApi

import controllers.autoCompletion.CompletionBase
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspace.doc.WorkspaceApiDoc
import controllers.workspace.workspaceRequests.{VocabularyInfo, VocabularyInfos, VocabularyLookupRequest, VocabularyLookupResponse, VocabularyLookupResult}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.Prefixes
import org.silkframework.rule.vocab.{GenericInfo, VocabularyProperty}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.{StringUtils, Uri}
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

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
    summary = "Look up types and properties in the global vocabulary cache.",
    description = "Resolves a batch of absolute URIs or prefixed names against the global vocabulary cache. " +
      "Results are returned one-to-one in request order. Invalid values are reported per item and do not fail the whole request.",
    method = "POST",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Vocabulary lookup results.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[VocabularyLookupResponse])
        ))
      ),
      new ApiResponse(
        responseCode = "400",
        description = "The request body could not be parsed."
      )
    )
  )
  @RequestBody(
    description = "Batch vocabulary lookup request.",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[VocabularyLookupRequest])
      )
    )
  )
  def lookup(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    validateJson[VocabularyLookupRequest] { lookupRequest =>
      implicit val prefixes: Prefixes = lookupRequest.projectId.filter(_.nonEmpty)
        .map(getProject(_).config.prefixes)
        .getOrElse(Prefixes.default)
      val vocabularies = VocabularyCacheValue.globalVocabularies
      val results = lookupRequest.uris.map(uri => lookupValue(uri, vocabularies))
      Ok(Json.toJson(VocabularyLookupResponse(results)))
    }
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
                                            name = "projectId",
                                            description = "Optional project ID in order to prefix properties with project prefixes.",
                                            required = false,
                                            in = ParameterIn.QUERY,
                                            schema = new Schema(implementation = classOf[String])
                                          )
                                          projectId: String,
                                          @Parameter(
                                            name = "limit",
                                            description = "The max. number of results.",
                                            required = true,
                                            in = ParameterIn.QUERY,
                                            schema = new Schema(implementation = classOf[String])
                                          )
                                          limit: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val cache = workspace.activity[GlobalVocabularyCache]
    implicit val prefixes: Prefixes = if(projectId.nonEmpty) getProject(projectId).config.prefixes else Prefixes.default

    cache.value.get match {
      case Some(gvc) =>
        val matches = findProperties(gvc, textQuery, limit)
        val sorted = matches.sortBy(_.info.labelValue)
        val completions = sorted.map(p => {
          val label = p.info.labelValue
          val uri = prefixes.shorten(p.info.uri)
          CompletionBase(
            value = uri,
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
                             limit: Int)
                            (implicit prefixes: Prefixes): Seq[VocabularyProperty] = {
    val results = ArrayBuffer[VocabularyProperty]()
    val searchWords = StringUtils.extractSearchTerms(textQuery).map(_.toLowerCase)
    for (vocabulary <- gvc.vocabularies;
         property <- vocabulary.properties) {
      val info = property.info
      val searchContent = s"${prefixes.shorten(info.uri)} ${info.labelValue}"
      if(StringUtils.matchesSearchTerm(searchWords, searchContent)) {
        results.append(property)
      }
      if (results.size >= limit) {
        return results.toSeq
      }
    }
    results.toSeq
  }

  private def lookupValue(input: String, vocabularies: Seq[org.silkframework.rule.vocab.Vocabulary])
                         (implicit prefixes: Prefixes): VocabularyLookupResult = {
    parseAbsoluteOrPrefixedUri(input) match {
      case Some(absoluteUri) =>
        resolveVocabularyEntry(absoluteUri, vocabularies) match {
          case Some((kind, info)) =>
            createResolvedLookupResult(input, absoluteUri, kind, info)
          case None =>
            VocabularyLookupResult(
              input = input,
              resolved = false,
              invalid = false,
              uri = absoluteUri,
              prefixedUri = prefixedUri(absoluteUri)
            )
        }
      case None =>
        VocabularyLookupResult(
          input = input,
          resolved = false,
          invalid = true,
          uri = input
        )
    }
  }

  private def createResolvedLookupResult(input: String,
                                         absoluteUri: String,
                                         kind: String,
                                         info: GenericInfo)
                                        (implicit prefixes: Prefixes): VocabularyLookupResult = {
    VocabularyLookupResult(
      input = input,
      resolved = true,
      invalid = false,
      uri = absoluteUri,
      kind = Some(kind),
      label = info.label.orElse(info.altLabels.headOption),
      description = info.description,
      prefixedUri = prefixedUri(absoluteUri),
      graphUri = info.vocabularyUri
    )
  }

  private def resolveVocabularyEntry(absoluteUri: String,
                                     vocabularies: Seq[org.silkframework.rule.vocab.Vocabulary]): Option[(String, GenericInfo)] = {
    vocabularies.collectFirst(Function.unlift { vocabulary =>
      vocabulary.getClass(absoluteUri).map(vocabularyClass => "class" -> vocabularyClass.info)
    }).orElse {
      vocabularies.collectFirst(Function.unlift { vocabulary =>
        vocabulary.getProperty(absoluteUri).map(vocabularyProperty => "property" -> vocabularyProperty.info)
      })
    }
  }

  private def prefixedUri(absoluteUri: String)
                         (implicit prefixes: Prefixes): Option[String] = {
    val shortened = prefixes.shorten(absoluteUri)
    Option.when(shortened != absoluteUri)(shortened)
  }

  private def parseAbsoluteOrPrefixedUri(input: String)
                                        (implicit prefixes: Prefixes): Option[String] = {
    val trimmed = input.trim
    if(trimmed.isEmpty) {
      None
    } else if(Uri(trimmed).isValidUri) {
      Some(trimmed)
    } else {
      Try(Uri.parse(trimmed, prefixes).uri)
        .toOption
        .filter(Uri(_).isValidUri)
    }
  }
}
