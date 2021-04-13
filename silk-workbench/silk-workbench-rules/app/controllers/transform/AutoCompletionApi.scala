package controllers.transform

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.transform.AutoCompletionApi.Categories
import controllers.transform.autoCompletion._
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{PathOperator, _}
import org.silkframework.entity.{ValueType, ValueTypeAnnotation}
import org.silkframework.rule.vocab.ObjectPropertyType
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCacheValue}
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import java.util.logging.Logger
import javax.inject.Inject
import scala.language.implicitConversions

/**
  * Generates auto completions for mapping paths and types.
  */
class AutoCompletionApi @Inject() () extends InjectedController with ControllerUtilsTrait {
  val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * Given a search term, returns all possible completions for source property paths.
    */
  def sourcePaths(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    val task = project.task[TransformSpec](taskName)
    var completions = Completions()
    withRule(task, ruleName) { case (_, sourcePath) =>
      val simpleSourcePath = simplePath(sourcePath)
      val forwardOnlySourcePath = forwardOnlyPath(simpleSourcePath)
      val allPaths = pathsCacheCompletions(task, simpleSourcePath)
      val isRdfInput = task.activity[TransformPathsCache].value().isRdfInput(task)
      // FIXME: No only generate relative "forward" paths, but also generate paths that would be accessible by following backward paths.
      val relativeForwardPaths = relativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths, isRdfInput)
      // Add known paths
      completions += relativeForwardPaths
      // Return filtered result
      Ok(completions.filterAndSort(term, maxResults, sortEmptyTermResult = false).toJson)
    }
  }

  private def simplePath(sourcePath: List[PathOperator]) = {
    sourcePath.filter(op => op.isInstanceOf[ForwardOperator] || op.isInstanceOf[BackwardOperator])
  }

  final val DEFAULT_AUTO_COMPLETE_RESULTS = 30

  /** A more fine-grained auto-completion of a source path that suggests auto-completion in parts of a path. */
  def partialSourcePath(projectId: String,
                        transformTaskId: String,
                        ruleId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext =>
      val (project, transformTask) = projectAndTask[TransformSpec](projectId, transformTaskId)
      implicit val prefixes: Prefixes = project.config.prefixes
      validateJson[PartialSourcePathAutoCompletionRequest] { autoCompletionRequest =>
        withRule(transformTask, ruleId) { case (_, sourcePath) =>
          val simpleSourcePath = simplePath(sourcePath)
          val forwardOnlySourcePath = forwardOnlyPath(simpleSourcePath)
          val allPaths = pathsCacheCompletions(transformTask, simpleSourcePath)
          val isRdfInput = transformTask.activity[TransformPathsCache].value().isRdfInput(transformTask)
          var relativeForwardPaths = relativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths, isRdfInput)
          val openWorld = false  // TODO: set openWorld correctly
          val pathToReplace = PartialSourcePathAutocompletionHelper.pathToReplace(autoCompletionRequest, openWorld)
          if(!openWorld && pathToReplace.from > 0) {
            val pathBeforeReplacement = UntypedPath.parse(autoCompletionRequest.inputString.take(pathToReplace.from))
            val simplePathBeforeReplacement = simplePath(pathBeforeReplacement.operators)
            relativeForwardPaths = relativePaths(simplePathBeforeReplacement, forwardOnlyPath(simplePathBeforeReplacement), relativeForwardPaths, isRdfInput)
          }
          // Add known paths
          val completions: Completions = relativeForwardPaths
          val from = pathToReplace.from
          val length = pathToReplace.length
          // Return filtered result
          val filteredResults = completions.
            filterAndSort(
              pathToReplace.query.map(_.mkString(" ")).getOrElse(""),
              autoCompletionRequest.maxSuggestions.getOrElse(DEFAULT_AUTO_COMPLETE_RESULTS),
              sortEmptyTermResult = false,
              multiWordFilter = true
            )
          val response = PartialSourcePathAutoCompletionResponse(
            autoCompletionRequest.inputString,
            autoCompletionRequest.cursorPosition,
            Some(ReplacementInterval(from, length)),
            pathToReplace.query.map(_.mkString(" ")).getOrElse(""),
            filteredResults.toCompletionsBase
          )
          Ok(Json.toJson(response))
        }
      }
  }

  private def withRule[T](transformTask: ProjectTask[TransformSpec],
                          ruleId: String)
                         (block: ((TransformRule, List[PathOperator])) => T): T = {
    transformTask.nestedRuleAndSourcePath(ruleId) match {
      case Some(value) =>
        block(value)
      case None =>
        throw new NotFoundException("Requesting auto-completion for non-existent rule " + ruleId + " in transformation task " + transformTask.fullTaskLabel + "!")
    }
  }

  /** Filter out paths that start with either the simple source or forward only source path, then
    * rewrite the auto-completion to a relative path from the full paths. */
  private def relativePaths(simpleSourcePath: List[PathOperator],
                            forwardOnlySourcePath: List[PathOperator],
                            pathCacheCompletions: Completions,
                            isRdfInput: Boolean)
                           (implicit prefixes: Prefixes): Seq[Completion] = {
    pathCacheCompletions.values.filter { p =>
      val path = UntypedPath.parse(p.value)
      isRdfInput || // FIXME: Currently there are no paths longer 1 in cache, that why return full path
      path.operators.startsWith(forwardOnlySourcePath) && path.operators.size > forwardOnlySourcePath.size ||
      path.operators.startsWith(simpleSourcePath) && path.operators.size > simpleSourcePath.size
    } map { completion =>
      val path = UntypedPath.parse(completion.value)
      val truncatedOps = if (path.operators.startsWith(forwardOnlySourcePath)) {
        path.operators.drop(forwardOnlySourcePath.size)
      } else if(isRdfInput) {
        path.operators
      } else {
        path.operators.drop(simpleSourcePath.size)
      }
      completion.copy(value = UntypedPath(truncatedOps).serialize())
    }
  }

  // Normalize this path by eliminating backward operators
  private def forwardOnlyPath(simpleSourcePath: List[PathOperator]): List[PathOperator] = {
    // Remove BackwardOperators
    var pathStack = List.empty[PathOperator]
    for (op <- simpleSourcePath) {
      op match {
        case f: ForwardOperator =>
          pathStack ::= f
        case BackwardOperator(prop) =>
          if (pathStack.isEmpty) {
            // TODO: What to do?
          } else {
            pathStack = pathStack.tail
          }
        case _ =>
          throw new IllegalArgumentException("Path cannot contain path operators other than forward and backward operators!")
      }
    }
    pathStack.reverse

  }

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def targetProperties(projectName: String,
                       taskName: String,
                       ruleName: String,
                       term: String,
                       maxResults: Int,
                       fullUris: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    var vocabularyFilter: Option[Seq[String]] = None
    if(request.hasBody) {
      request.body.asJson.foreach { json =>
        val request = JsonHelpers.fromJsonValidated[TargetPropertyAutoCompleteRequest](json)
        vocabularyFilter = request.vocabularies
      }
    }
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyPropertyCompletions(task, fullUris, vocabularyFilter)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults, multiWordFilter = true).toJson)
  }

  /**
    * Given a search term, returns possible completions for target types.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def targetTypes(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyTypeCompletions(task)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults).toJson)
  }

  /**
    * Given a search term, returns possible completions for value types.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def valueTypes(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val valueTypeBlacklist = Set(ValueType.CUSTOM_VALUE_TYPE_ID)
    val valueTypes = PluginRegistry.availablePlugins[ValueType].sortBy(_.label)
    val filteredValueTypes = valueTypes.filterNot(v => valueTypeBlacklist.contains(v.id))
    val completions = Completions(filteredValueTypes.map(valueTypeCompletion))
    Ok(completions.filterAndSort(term, maxResults, sortEmptyTermResult = false).toJson)
  }

  private def valueTypeCompletion(valueType: PluginDescription[ValueType]): Completion = {
    val annotation = valueType.pluginClass.getAnnotation(classOf[ValueTypeAnnotation])
    val annotationDescription =
      if(annotation != null) {
        val validValues = annotation.validValues().map(str => s"'$str'").mkString(", ")
        val invalidValues = annotation.invalidValues().map(str => s"'$str'").mkString(", ")
        s" Examples for valid values are: $validValues. Invalid values are: $invalidValues."
      } else {
        ""
      }

    Completion(
      value = valueType.id,
      label = Some(valueType.label),
      description = Some(valueType.description + annotationDescription),
      category = valueType.categories.headOption.getOrElse(""),
      isCompletion = true
    )
  }

  private def pathsCacheCompletions(task: ProjectTask[TransformSpec], sourcePath: List[PathOperator])
                                   (implicit userContext: UserContext): Completions = {
    if (Option(task.activity[TransformPathsCache].value).isDefined) {
      val paths = fetchCachedPaths(task, sourcePath)
      val serializedPaths = paths.map(_.toUntypedPath.serialize()(task.project.config.prefixes)).sorted.distinct
      for(pathStr <- serializedPaths) yield {
        Completion(
          value = pathStr,
          label = None,
          description = None,
          category = Categories.sourcePaths,
          isCompletion = true
        )
      }
    } else {
      Completions()
    }
  }

  private def fetchCachedPaths(task: ProjectTask[TransformSpec], sourcePath: List[PathOperator])
                              (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val cachedSchemata = task.activity[TransformPathsCache].value()
    cachedSchemata.fetchCachedPaths(task, sourcePath)
  }

  private def vocabularyTypeCompletions(task: ProjectTask[TransformSpec])
                                       (implicit userContext: UserContext): Completions = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = VocabularyCacheValue.targetVocabularies(task)

    val typeCompletions =
      for(vocab <- vocabularyCache.vocabularies; vocabClass <- vocab.classes) yield {
        Completion(
          value = prefixes.shorten(vocabClass.info.uri.toString),
          label = vocabClass.info.label,
          description = vocabClass.info.description,
          category = Categories.vocabularyTypes,
          isCompletion = true
        )
      }

    typeCompletions.distinct
  }

  private def vocabularyPropertyCompletions(task: ProjectTask[TransformSpec],
                                           // Return full URIs instead of prefixed URIs
                                            fullUris: Boolean,
                                           // Filter by vocabularies if non-empty
                                            vocabularyFilter: Option[Seq[String]])
                                           (implicit userContext: UserContext): Completions = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = VocabularyCacheValue.targetVocabularies(task, vocabularyFilter.filter(_.nonEmpty))

    val propertyCompletions =
      for(vocab <- vocabularyCache.vocabularies; prop <- vocab.properties) yield {
        Completion(
          value = if(fullUris) prop.info.uri else prefixes.shorten(prop.info.uri),
          label = prop.info.label,
          description = prop.info.description,
          category = Categories.vocabularyProperties,
          isCompletion = true,
          extra = Some(Json.obj(
            "type" -> (if (prop.propertyType == ObjectPropertyType) "object" else "value"),
            "graph" -> vocab.info.uri // FIXME: Currently the vocab URI and graph URI are the same. This might change in the future.
          ))
        )
      }

    propertyCompletions.distinct
  }


  private implicit def createCompletion(completions: Seq[Completion]): Completions = Completions(completions)

}

object AutoCompletionApi {

  /**
    * The names of the auto completion categories.
    */
  object Categories {

    val prefixes = "Prefixes"

    val sourcePaths = "Source Paths"

    val partialSourcePaths = "Partial Source Paths"

    val vocabularyTypes = "Vocabulary Types"

    val vocabularyProperties = "Vocabulary Properties"

  }

}


