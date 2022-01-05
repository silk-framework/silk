package controllers.workspaceApi

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.workspaceApi.doc.WorkspaceUriPatternApiDoc
import controllers.workspaceApi.uriPattern.{UriPatternRequest, UriPatternResponse, UriPatternResult}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.{DefaultConfig, Prefixes}
import org.silkframework.entity.paths.{DirectionalPathOperator, UntypedPath}
import org.silkframework.rule.util.UriPatternParser.{ConstantPart, PathPart}
import org.silkframework.rule.util.{UriPatternParser, UriPatternSegments}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Uri
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.GlobalWorkspaceActivity
import org.silkframework.workspace.activity.transform.{GlobalUriPatternCache, GlobalUriPatternCacheValue}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, InjectedController}

import java.time.{Duration, Instant}
import scala.collection.mutable
import scala.util.Try

@Tag(name = "Workspace URI patterns")
class WorkspaceUriPatternApi extends InjectedController with UserContextActions with ControllerUtilsTrait {
  @Operation(
    summary = "Find URI patterns",
    description = "List all URI patterns that are in use for the given target classes. At least one class must be provided.",
    method = "POST",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "URI pattern results",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(WorkspaceUriPatternApiDoc.uriPatternsExample))
        )
        )
      )
    ))
  @RequestBody(
    description = "URI patterns request",
    required = true,
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[UriPatternRequest], example = WorkspaceUriPatternApiDoc.uriPatternsRequest)
      ))
  )
  def uriPatterns(): Action[JsValue] = RequestUserContextAction(parse.json) { implicit r => implicit userContext =>
    validateJson[UriPatternRequest] { request =>
      implicit val prefixes: Prefixes = getProject(request.projectId).config.prefixes
      if(request.targetClassUris.isEmpty) {
        throw new BadUserInputException("At least one target class must be specified!")
      }
      val absoluteTargetClassUris = request.targetClassUris.map(uri => Try(Uri.parse(uri, prefixes).uri).getOrElse(uri))
      globalUriPatternCacheValue match {
        case Some(cacheValue) =>
          val uriPatternResults = for(targetClass <- absoluteTargetClassUris;
              uriPattern <- cacheValue.uriPatterns.get(targetClass).toSeq.flatten;
              uriPatternSegments <- Try(UriPatternParser.parseIntoSegments(uriPattern, allowIncompletePattern = false)).toOption) yield {
            val label = uriPatternLabel(uriPatternSegments)
            UriPatternResult(targetClass, Some(label), uriPattern)
          }
          if(request.uniqueValues.getOrElse(false)) {
            val existingPatterns = mutable.HashSet[String]()
            val distinctPatternResult = uriPatternResults.filter(pattern => {
              val alreadyExists = existingPatterns.contains(pattern.value)
              existingPatterns.add(pattern.value)
              !alreadyExists
            })
            Ok(Json.toJson(UriPatternResponse(distinctPatternResult)))
          } else {
            Ok(Json.toJson(UriPatternResponse(uriPatternResults)))
          }
        case None =>
          Ok(Json.toJson(UriPatternResponse(results = Seq.empty)))
      }
    }
  }

  // A simplified URI pattern for display to a user, i.e. simplified path expressions.
  private def uriPatternLabel(uriPatternSegments: UriPatternSegments): String = {
    val stringSegments = uriPatternSegments.segments map {
      case ConstantPart(value, _) =>
        value
      case PathPart(path, _) =>
        UntypedPath.parse(path).operators.
          filter(_.isInstanceOf[DirectionalPathOperator]).
          map(_.asInstanceOf[DirectionalPathOperator].property).
          lastOption match {
          case Some(pathPart) =>
            pathPart.localName match {
              case Some(localName) if path == localName =>
                // nothing shortened, display in original
                s"{$localName}"
              case Some(localName) =>
                s"{…$localName}"
              case None =>
                s"{…${pathPart.uri}}"
            }
          case None =>
            "{…}"
        }
    }
    stringSegments.mkString
  }

  private def globalUriPatternCacheValue(implicit userContext: UserContext): Option[GlobalUriPatternCacheValue] = {
    val cfg = DefaultConfig.instance.extendedTypesafeConfig()
    if (cfg.getBooleanOrElse(GlobalUriPatternCache.CONFIG_KEY_ENABLED, fallbackValue = false)) {
      val timeToRefresh = cfg.getDurationOrElse(GlobalUriPatternCache.CONFIG_KEY_TIME_BETWEEN_REFRESHES, Duration.ofSeconds(10))
      val globalUriPatternCache = WorkspaceFactory().workspace.activity[GlobalUriPatternCache]
      globalUriPatternCache.value.get match {
        case Some(value) if Instant.now isBefore value.lastUpdated.plus(timeToRefresh) =>
          // return recent enough value
          Some(value)
        case Some(value) =>
          // Try to fetch a new value or return old value if it takes too long
          val waitForNewValueTime = cfg.getDurationOrElse(GlobalUriPatternCache.CONFIG_KEY_WAIT_FOR_CACHE_TO_FINISH, Duration.ofMillis(50))
          fetchNewValueCapped(globalUriPatternCache, value, waitForNewValueTime)
        case None =>
          // Empty cache, first request, start blocking.
          startGlobalUriPatternCache(globalUriPatternCache)
          globalUriPatternCache.control.waitUntilFinished()
          globalUriPatternCache.value.get
      }
    } else {
      None
    }
  }

  // Starts the synonym cache and waits at most for a specific amount of time for a new value being available.
  private def fetchNewValueCapped(globalSynonymCache: GlobalWorkspaceActivity[GlobalUriPatternCache],
                                  latestValue: GlobalUriPatternCacheValue,
                                  waitForNewValueTime: Duration)
                                 (implicit userContext: UserContext): Option[GlobalUriPatternCacheValue] = {
    // Old value, try to fetch a more recent one
    startGlobalUriPatternCache(globalSynonymCache)
    val start = Instant.now()
    def valueUnchanged(): Boolean = globalSynonymCache.control.value.get.forall(_.lastUpdated == latestValue.lastUpdated)
    while (valueUnchanged() && (Instant.now isBefore start.plus(waitForNewValueTime))) {
      Thread.sleep(5)
    }
    globalSynonymCache.control.value.get match {
      case Some(newValue) if newValue.lastUpdated isAfter latestValue.lastUpdated =>
        Some(newValue)
      case _ =>
        Some(latestValue)
    }
  }

  private def startGlobalUriPatternCache(globalUriPatternCache: GlobalWorkspaceActivity[GlobalUriPatternCache])
                                        (implicit userContext: UserContext): Unit = {
    try {
      globalUriPatternCache.control.start()
    } catch {
      case _: IllegalStateException => // Ignore exceptions because of parallel starts of the activity
    }
  }
}
