package controllers.transform

import controllers.core.util.ControllerUtilsTrait
import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.transform.AutoCompletionApi.Categories
import controllers.transform.autoCompletion._
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSourceCharacteristics
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.paths.{PathOperator, _}
import org.silkframework.entity.{ValueType, ValueTypeAnnotation}
import org.silkframework.rule.vocab.ObjectPropertyType
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
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

  private def simplePath(sourcePath: List[PathOperator]): List[PathOperator] = {
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
        validatePartialSourcePathAutoCompletionRequest(autoCompletionRequest)
        withRule(transformTask, ruleId) { case (_, sourcePath) =>
          val simpleSourcePath = simplePath(sourcePath)
          val forwardOnlySourcePath = forwardOnlyPath(simpleSourcePath)
          val allPaths = pathsCacheCompletions(transformTask, simpleSourcePath)
          val isRdfInput = transformTask.activity[TransformPathsCache].value().isRdfInput(transformTask)
          val dataSourceCharacteristicsOpt = dataSourceCharacteristics(transformTask)
          var relativeForwardPaths = relativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths, isRdfInput)
          val pathToReplace = PartialSourcePathAutocompletionHelper.pathToReplace(autoCompletionRequest, isRdfInput)
          if(pathToReplace.from > 0) {
            if(!isRdfInput) {
              // compute relative paths
              val pathBeforeReplacement = UntypedPath.partialParse(autoCompletionRequest.inputString.take(pathToReplace.from)).partialPath
              val simplePathBeforeReplacement = simplePath(pathBeforeReplacement.operators)
              relativeForwardPaths = relativePaths(simplePathBeforeReplacement, forwardOnlyPath(simplePathBeforeReplacement),
                relativeForwardPaths, isRdfInput, oneHopOnly = pathToReplace.insideFilter, serializeFull = !pathToReplace.insideFilter)
            } else if(isRdfInput && !pathToReplace.insideFilter) {
              // add forward operator for RDF paths when not in filter
              relativeForwardPaths = relativeForwardPaths.map(c => if(!c.value.startsWith("/") && !c.value.startsWith("\\")) c.copy(value = "/" + c.value) else c)
            }
          }
          val dataSourceSpecialPathCompletions = specialPathCompletions(dataSourceCharacteristicsOpt, pathToReplace)
          // Add known paths
          val completions: Completions = relativeForwardPaths ++ dataSourceSpecialPathCompletions
          val from = pathToReplace.from
          val length = pathToReplace.length
          // Return filtered result
          val filteredResults = filterResults(autoCompletionRequest, pathToReplace, completions, dataSourceSpecialPathCompletions)
          val response = PartialSourcePathAutoCompletionResponse(
            autoCompletionRequest.inputString,
            autoCompletionRequest.cursorPosition,
            replacementResults = Seq(
              ReplacementResults(
                ReplacementInterval(from, length),
                pathToReplace.query.getOrElse(""),
                filteredResults.toCompletionsBase.completions
              )
            ) ++ operatorCompletions(dataSourceCharacteristicsOpt, pathToReplace, autoCompletionRequest)
          )
          Ok(Json.toJson(response))
        }
      }
  }

  private def specialPathCompletions(dataSourceCharacteristicsOpt: Option[DataSourceCharacteristics],
                                     pathToReplace: PathToReplace): Seq[Completion] = {
    if(pathToReplace.insideQuotes) {
      Seq.empty
    } else {
      dataSourceCharacteristicsOpt.toSeq.flatMap { characteristics =>
        val pathOps = Seq("/", "\\", "[")

        def pathWithoutOperator(specialPath: String): Boolean = pathOps.forall(op => !specialPath.startsWith(op))

        characteristics.supportedPathExpressions.specialPaths
          // No backward or filter paths allowed inside filters
          .filter(p => !pathToReplace.insideFilter || !pathOps.drop(1).forall(disallowedOp => p.value.startsWith(disallowedOp)))
          .map { p =>
            val pathSegment = if (pathToReplace.from > 0 && !pathToReplace.insideFilter && pathWithoutOperator(p.value)) {
              "/" + p.value
            } else if ((pathToReplace.from == 0 || pathToReplace.insideFilter) && p.value.startsWith("/")) {
              p.value.stripPrefix("/")
            } else {
              p.value
            }
            Completion(pathSegment, label = None, description = p.description, category = Categories.sourcePaths, isCompletion = true)
          }
      }
    }
  }

  // Filter results based on text query and limit number of results
  private def filterResults(autoCompletionRequest: PartialSourcePathAutoCompletionRequest,
                            pathToReplace: PathToReplace,
                            completions: Completions,
                            dataSourceSpecialPathCompletions: Seq[Completion]): Completions = {
    val stringToBeReplaced = autoCompletionRequest.inputString.substring(pathToReplace.from, pathToReplace.from + pathToReplace.length)
    val filteredCompletions: Completions = pathToReplace.query match {
      case Some(query) =>
        completions.
          filterAndSort(
            query,
            autoCompletionRequest.maxSuggestions.getOrElse(DEFAULT_AUTO_COMPLETE_RESULTS),
            sortEmptyTermResult = false,
            multiWordFilter = true
          )
      case None =>
        Seq.empty
    }
    if(filteredCompletions.values.size == 1 && filteredCompletions.values.head.value == stringToBeReplaced) {
      // If only real search result has the exact same value as the string to be replaced, do not suggest anything.
      Seq.empty
    } else {
      filteredCompletions
    }
  }

  private def operatorCompletions(dataSourceCharacteristicsOpt: Option[DataSourceCharacteristics],
                                  pathToReplace: PathToReplace,
                                  autoCompletionRequest: PartialSourcePathAutoCompletionRequest): Option[ReplacementResults] = {
    def completion(predicate: Boolean, value: String, description: String): Option[CompletionBase] = {
      if(predicate) Some(CompletionBase(value, description = Some(description))) else None
    }
    // Propose operators
    if (!pathToReplace.insideFilter && !pathToReplace.insideQuotesOrUri
      && !autoCompletionRequest.charBeforeCursor.contains('/') && !autoCompletionRequest.charBeforeCursor.contains('\\')) {
      val supportedPathExpressions = dataSourceCharacteristicsOpt.getOrElse(DataSourceCharacteristics()).supportedPathExpressions
      val forwardOp = completion(autoCompletionRequest.cursorPosition > 0 && supportedPathExpressions.multiHopPaths, "/", "Starts a forward path segment")
      val backwardOp = completion(supportedPathExpressions.backwardPaths && supportedPathExpressions.multiHopPaths, "\\", "Starts a backward path segment")
      val langFilterOp = completion(autoCompletionRequest.cursorPosition > 0 && supportedPathExpressions.languageFilter, "[@lang ", "Starts a language filter expression")
      val propertyFilter = completion(autoCompletionRequest.cursorPosition > 0  && supportedPathExpressions.propertyFilter, "[", "Starts a property filter expression")
      Some(ReplacementResults(
        ReplacementInterval(autoCompletionRequest.cursorPosition, 0),
        "",
        forwardOp.toSeq ++ backwardOp ++ propertyFilter ++ langFilterOp
      ))
    }
    else {
      None
    }
  }

  private def dataSourceCharacteristics(task: ProjectTask[TransformSpec])
                (implicit userContext: UserContext): Option[DataSourceCharacteristics] = {
    task.project.taskOption[GenericDatasetSpec](task.selection.inputId)
      .map(_.data.source.characteristics)
  }

  private def validatePartialSourcePathAutoCompletionRequest(request: PartialSourcePathAutoCompletionRequest): Unit = {
    var error = ""
    if(request.cursorPosition < 0) error = "Cursor position must be >= 0"
    if(request.maxSuggestions.nonEmpty && request.maxSuggestions.get <= 0) error = "Max suggestions must be larger zero"
    if(error != "") {
      throw BadUserInputException(error)
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
                            isRdfInput: Boolean,
                            oneHopOnly: Boolean = false,
                            serializeFull: Boolean = false)
                           (implicit prefixes: Prefixes): Seq[Completion] = {
    pathCacheCompletions.values.filter { p =>
      val path = UntypedPath.parse(p.value)
      val matchesPrefix = isRdfInput || // FIXME: Currently there are no paths longer 1 in cache, that why return full path
        path.operators.startsWith(forwardOnlySourcePath) && path.operators.size > forwardOnlySourcePath.size ||
        path.operators.startsWith(simpleSourcePath) && path.operators.size > simpleSourcePath.size
      val truncatedOps = truncatePath(path, simpleSourcePath, forwardOnlySourcePath, isRdfInput)
      matchesPrefix && (!oneHopOnly || truncatedOps.size == 1)
    } map { completion =>
      val path = UntypedPath.parse(completion.value)
      val truncatedOps = truncatePath(path, simpleSourcePath, forwardOnlySourcePath, isRdfInput)
      completion.copy(value = UntypedPath(truncatedOps).serialize(stripForwardSlash = !serializeFull))
    }
  }

  private def truncatePath(path: UntypedPath,
                           simpleSourcePath: List[PathOperator],
                           forwardOnlySourcePath: List[PathOperator],
                           isRdfInput: Boolean): List[PathOperator] = {
    if (path.operators.startsWith(forwardOnlySourcePath)) {
      path.operators.drop(forwardOnlySourcePath.size)
    } else if(isRdfInput) {
      path.operators
    } else {
      path.operators.drop(simpleSourcePath.size)
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
    val annotationDescription = {
      if(annotation != null) {
        val validValues = annotation.validValues().map(str => s"'$str'").mkString(", ")
        val invalidValues = annotation.invalidValues().map(str => s"'$str'").mkString(", ")
        s" Examples for valid values are: $validValues. Invalid values are: $invalidValues."
      } else {
        ""
      }
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
      val serializedPaths = paths
        // Sort primarily by path operator length then name
        .sortWith { (p1, p2) =>
          if (p1.operators.length == p2.operators.length) {
            p1.serialize() < p2.serialize()
          } else {
            p1.operators.length < p2.operators.length
          }
        }
        .map(_.toUntypedPath.serialize()(task.project.config.prefixes))
        .distinct
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


