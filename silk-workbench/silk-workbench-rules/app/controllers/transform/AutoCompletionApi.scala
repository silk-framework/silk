package controllers.transform

import controllers.autoCompletion.{AutoSuggestAutoCompletionRequest, AutoSuggestAutoCompletionResponse, Completion, Completions}
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.shared.autoCompletion.AutoCompletionApiUtils
import controllers.transform.AutoCompletionApi.{Categories, InputAndOutputTasks}
import controllers.transform.autoCompletion._
import controllers.transform.doc.AutoCompletionApiDoc
import controllers.transform.transformTask.TransformUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.{FixedNumberOfInputs, FixedSchemaPort, FlexibleNumberOfInputs, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetCharacteristics
import org.silkframework.entity.paths._
import org.silkframework.entity.{EntitySchema, ValueType, ValueTypeAnnotation}
import org.silkframework.rule.vocab.ObjectPropertyType
import org.silkframework.rule.{ContainerTransformRule, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCacheValue}
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowTaskContext, WorkflowTaskContextInputTask, WorkflowTaskContextOutputTask, WorkflowTaskContextTask}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import java.util.logging.Logger
import javax.inject.Inject
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

/**
  * Generates auto completions for mapping paths and types.
  */
@Tag(name = "Transform autocompletion", description = "Autocomplete types and paths in transformations.")
class AutoCompletionApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  val log: Logger = Logger.getLogger(this.getClass.getName)

  @Operation(
    summary = "Mapping rule source paths",
    description = "Given a search term, returns all possible completions for source property paths.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Source paths that match the given term",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Completions]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.pathCompletionExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
  ))
  def sourcePaths(@Parameter(name = "project", description = "The project identifier", in = ParameterIn.PATH,
                    required = true,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "task",
                    description = "The task identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  taskName: String,
                  @Parameter(
                    name = "rule",
                    description = "The rule identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  ruleName: String,
                  @Parameter(
                    name = "term",
                    description = "The search term. Will also return non-exact matches (e.g., naMe == name) and matches from labels.",
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String], defaultValue = "")
                  )
                  term: String,
                  @Parameter(
                    name = "maxResults",
                    description = "The maximum number of results.",
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[Int], defaultValue = "30")
                  )
                  maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    val task = project.task[TransformSpec](taskName)
    var completions = Completions()
    withRule(task, ruleName) { case (_, sourcePath) =>
      val isRdfInput = TransformUtils.isRdfInput(task)
      val simpleSourcePath = AutoCompletionApiUtils.simplePath(sourcePath)
      val forwardOnlySourcePath = AutoCompletionApiUtils.forwardOnlyPath(simpleSourcePath)
      val allPaths = AutoCompletionApiUtils.pathsCacheCompletions(task.selection.typeUri, task.activity[TransformPathsCache].value.get, simpleSourcePath.nonEmpty && isRdfInput)
      // FIXME: No only generate relative "forward" paths, but also generate paths that would be accessible by following backward paths.
      val relativeForwardPaths = AutoCompletionApiUtils.extractRelativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths, isRdfInput)
      // Add known paths
      completions += relativeForwardPaths
      // Return filtered result
      Ok(completions.filterAndSort(term, maxResults, sortEmptyTermResult = false, multiWordFilter = true).toJson)
    }
  }

  @Operation(
    summary = "Mapping rule partial source paths",
    description = "Returns auto-completion suggestions based on a complex path expression (including backward paths and filters) and the cursor position. The results may only replace a part of the original input string.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = AutoCompletionApiDoc.partialSourcePathsResponseDescription,
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[AutoSuggestAutoCompletionResponse]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.partialSourcePathsResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
  ))
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[PartialSourcePathAutoCompletionRequest]),
        examples = Array(new ExampleObject(AutoCompletionApiDoc.partialSourcePathsRequestExample))
      )
    )
  )
  def partialSourcePath(@Parameter(name = "project", description = "The project identifier", in = ParameterIn.PATH,
                          required = true,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectId: String,
                        @Parameter(name = "task", description = "The task identifier", in = ParameterIn.PATH,
                          required = true,
                          schema = new Schema(implementation = classOf[String])
                        )
                        transformTaskId: String,
                        @Parameter(name = "rule", description = "The rule identifier", in = ParameterIn.PATH,
                          required = true,
                          schema = new Schema(implementation = classOf[String])
                        )
                        ruleId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext =>
      val (project, transformTask) = projectAndTask[TransformSpec](projectId, transformTaskId)
      validateJson[PartialSourcePathAutoCompletionRequest] { autoCompletionRequest =>
        AutoCompletionApi.validateAutoCompletionRequest(autoCompletionRequest)
        val inputAndOutputTasks = AutoCompletionApi.validateWorkflowContext(project, autoCompletionRequest.taskContext)
        withRule(transformTask, ruleId) { case (_, sourcePath) =>
          val autoCompletionResponse = autoCompletePartialSourcePath(transformTask, autoCompletionRequest, sourcePath,
            autoCompletionRequest.isObjectPath.getOrElse(false), inputAndOutputTasks.inputTasksSchema())
          Ok(Json.toJson(autoCompletionResponse))
        }
      }
  }

  // Returns an auto-completion result for a partial path request
  private def autoCompletePartialSourcePath(transformTask: ProjectTask[TransformSpec],
                                            autoCompletionRequest: PartialSourcePathAutoCompletionRequest,
                                            sourcePath: List[PathOperator],
                                            isObjectPath: Boolean,
                                            alternativeInputSchema: Option[EntitySchema])
                                           (implicit userContext: UserContext): AutoSuggestAutoCompletionResponse = {
    implicit val project: Project = transformTask.project
    implicit val prefixes: Prefixes = project.config.prefixes
    val isRdfInput = TransformUtils.isRdfInput(transformTask)
    val pathToReplace = PartialSourcePathAutocompletionHelper.pathToReplace(autoCompletionRequest, isRdfInput)
    val dataSourceCharacteristicsOpt = if(alternativeInputSchema.isEmpty) {
      TransformUtils.datasetCharacteristics(transformTask)
    } else {
      None
    }
    // compute relative paths
    val pathBeforeReplacement = UntypedPath.partialParse(autoCompletionRequest.inputString.take(pathToReplace.from)).partialPath
    val completeSubPath = sourcePath ++ pathBeforeReplacement.operators
    val simpleSubPath = AutoCompletionApiUtils.simplePath(completeSubPath)

    val forwardOnlySubPath = AutoCompletionApiUtils.forwardOnlyPath(simpleSubPath)
    val allPaths = AutoCompletionApiUtils.pathsCacheCompletions(transformTask.selection.typeUri, transformTask.activity[TransformPathsCache].value.get, simpleSubPath.nonEmpty && isRdfInput,
      alternativeInputSchema = alternativeInputSchema)
    val pathOpFilter = (autoCompletionRequest.isInBackwardOp, autoCompletionRequest.isInExplicitForwardOp) match {
      case (true, false) => OpFilter.Backward
      case (false, true) => OpFilter.Forward
      case _ => OpFilter.None
    }
    val relativePaths = AutoCompletionApiUtils.extractRelativePaths(simpleSubPath, forwardOnlySubPath, allPaths, isRdfInput, oneHopOnly = pathToReplace.insideFilter,
      serializeFull = !pathToReplace.insideFilter && pathToReplace.from > 0, pathOpFilter = pathOpFilter
    )
    val dataSourceSpecialPathCompletions = PartialSourcePathAutocompletionHelper.specialPathCompletions(dataSourceCharacteristicsOpt, pathToReplace, pathOpFilter, isObjectPath)
    // Add known paths
    val completions: Completions = relativePaths ++ dataSourceSpecialPathCompletions
    // Return filtered result
    val operatorCompletions = PartialSourcePathAutocompletionHelper.operatorCompletions(dataSourceCharacteristicsOpt, pathToReplace, autoCompletionRequest)
    val filteredResults = PartialSourcePathAutocompletionHelper.filterResults(autoCompletionRequest, pathToReplace, completions,
      filterOutSingleExactQueryStringCompletion = operatorCompletions.forall(_.replacements.isEmpty))
    AutoCompletionApiUtils.partialAutoCompletionResult(autoCompletionRequest, pathToReplace, operatorCompletions, filteredResults)
  }

  @Operation(
    summary = "URI pattern auto-completion",
    description = "Returns source path auto-completion suggestions when the cursor position is inside a path expression of a URI pattern. The results may only replace a part of the original input string.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = AutoCompletionApiDoc.partialSourcePathsResponseDescription,
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[AutoSuggestAutoCompletionResponse]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.partialSourcePathsResponseExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    ))
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        schema = new Schema(implementation = classOf[UriPatternAutoCompletionRequest]),
        examples = Array(new ExampleObject(AutoCompletionApiDoc.uriPatternAutoCompletionRequestExample))
      )
    )
  )
  def uriPattern(
                  @Parameter(name = "project", description = "The project identifier", in = ParameterIn.PATH,
                    required = true,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectId: String,
                  @Parameter(name = "task", description = "The task identifier", in = ParameterIn.PATH,
                    required = true,
                    schema = new Schema(implementation = classOf[String])
                  )
                  transformTaskId: String,
                  @Parameter(name = "rule", description = "The rule identifier", in = ParameterIn.PATH,
                    required = true,
                    schema = new Schema(implementation = classOf[String])
                  )
                  ruleId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
      val (project, transformTask) = projectAndTask[TransformSpec](projectId, transformTaskId)
      validateJson[UriPatternAutoCompletionRequest] { uriPatternAutoCompletionRequest =>
        AutoCompletionApi.validateAutoCompletionRequest(uriPatternAutoCompletionRequest)
        uriPatternAutoCompletionRequest.activePathPart match {
          case Some(pathPart) =>
            // Inside path expression, do path auto-completion
            withRule(transformTask, ruleId) { case (_, sourcePath) =>
              implicit val prefixes: Prefixes = transformTask.project.config.prefixes
              val basePath = sourcePath ++ uriPatternAutoCompletionRequest.objectPath.map(path => UntypedPath.parse(path).operators).getOrElse(Nil)
              val partialSourcePathAutoCompletionRequest = PartialSourcePathAutoCompletionRequest(
                pathPart.serializedPath,
                uriPatternAutoCompletionRequest.cursorPosition - pathPart.segmentPosition.originalStartIndex,
                uriPatternAutoCompletionRequest.maxSuggestions,
                Some(false),
                uriPatternAutoCompletionRequest.workflowTaskContext
              )
              val autoCompletionResponse = autoCompletePartialSourcePath(transformTask, partialSourcePathAutoCompletionRequest,
                basePath, isObjectPath = false, AutoCompletionApi.validateWorkflowContext(project, uriPatternAutoCompletionRequest.workflowTaskContext).inputTasksSchema())
              val offset = pathPart.segmentPosition.originalStartIndex
              Ok(Json.toJson(autoCompletionResponse.copy(
                inputString = uriPatternAutoCompletionRequest.inputString,
                cursorPosition = uriPatternAutoCompletionRequest.cursorPosition,
                // Fix replacement intervals
                replacementResults = autoCompletionResponse.replacementResults.map(replacementResult =>
                  replacementResult.copy(replacementInterval = replacementResult.replacementInterval.copy(from = replacementResult.replacementInterval.from + offset)))
              )))
            }
          case None =>
            Ok(Json.toJson(AutoSuggestAutoCompletionResponse(
              uriPatternAutoCompletionRequest.inputString,
              uriPatternAutoCompletionRequest.cursorPosition,
              Seq.empty
            )))
        }
      }
  }

  private def withRule[T](transformTask: ProjectTask[TransformSpec],
                          ruleId: String,
                          // When true the container rule is returned instead when a value rule was found.
                          firstContainerRule: Boolean = false)
                         (block: ((TransformRule, List[PathOperator])) => T): T = {
    transformTask.nestedRuleAndSourcePathWithParents(ruleId) match {
      case Nil =>
        throw new NotFoundException("Requesting auto-completion for non-existent rule " + ruleId + " in transformation task " + transformTask.fullLabel + "!")
      case result: List[(TransformRule, List[PathOperator])] =>
        val foundRule = result.last
        if(firstContainerRule && !foundRule._1.isInstanceOf[ContainerTransformRule]) {
          // The first container rule, i.e. the parent rule if the found rule was no container rule.
          block(result.dropRight(1).last)
        } else {
          // The found rule
          block(foundRule)
        }
    }
  }

  @Operation(
    summary = "Mapping rule target properties",
    description = "Given a search term, returns possible completions for target properties.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All properties from the target vocabulary that match the given term.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Completions]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.pathCompletionExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    ))
  def targetPropertiesGet( @Parameter(
                             name = "project",
                             description = "The project identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           projectName: String,
                           @Parameter(
                             name = "task",
                             description = "The task identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           taskName: String,
                           @Parameter(
                             name = "rule",
                             description = "The rule identifier",
                             required = true,
                             in = ParameterIn.PATH,
                             schema = new Schema(implementation = classOf[String])
                           )
                           ruleName: String,
                           @Parameter(
                             name = "term",
                             description = "The search term. Will also return non-exact matches (e.g., naMe == name) and matches from labels.",
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[String], defaultValue = "")
                           )
                           term: String,
                           @Parameter(
                             name = "maxResults",
                             description = "The maximum number of results.",
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Int], defaultValue = "30")
                           )
                           maxResults: Int,
                           @Parameter(
                             name = "fullUris",
                             description = "Return full URIs instead of prefixed ones.",
                             in = ParameterIn.QUERY,
                             schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                           )
                           fullUris: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    var vocabularyFilter: Option[Seq[String]] = None
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyPropertyCompletions(task, fullUris, vocabularyFilter)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults, multiWordFilter = true).toJson)
  }

  @Operation(
    summary = "Mapping rule target properties",
    description = "Given a search term, returns possible completions for target properties. In addition it allows to specify the vocabularies that should be used for auto-completion with the optional `vocabularies` parameter in the JSON body.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All properties from the target vocabulary that match the given term.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Completions]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.pathCompletionExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
  ))
  @RequestBody(
    content = Array(
      new Content(
        mediaType = "application/json",
        examples = Array(new ExampleObject("""{ "vocabularies": ["http://schema.org/", "http://xmlns.com/foaf/0.1/"] }""")),
        schema = new Schema(implementation = classOf[TargetPropertyAutoCompleteRequest])
    ))
  )
  def targetPropertiesPost( @Parameter(
                               name = "project",
                               description = "The project identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             projectName: String,
                             @Parameter(
                               name = "task",
                               description = "The task identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             taskName: String,
                             @Parameter(
                               name = "rule",
                               description = "The rule identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             ruleName: String,
                             @Parameter(
                               name = "term",
                               description = "The search term. Will also return non-exact matches (e.g., naMe == name) and matches from labels.",
                               in = ParameterIn.QUERY,
                               schema = new Schema(implementation = classOf[String], defaultValue = "")
                             )
                             term: String,
                             @Parameter(
                               name = "maxResults",
                               description = "The maximum number of results.",
                               in = ParameterIn.QUERY,
                               schema = new Schema(implementation = classOf[Int], defaultValue = "30")
                             )
                             maxResults: Int,
                             @Parameter(
                               name = "fullUris",
                               description = "Return full URIs instead of prefixed ones.",
                               required = false,
                               in = ParameterIn.QUERY,
                               schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                             )
                             fullUris: Boolean): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    var vocabularyFilter: Option[Seq[String]] = None
    request.body.asJson.foreach { json =>
      val request = JsonHelpers.fromJsonValidated[TargetPropertyAutoCompleteRequest](json)
      vocabularyFilter = request.vocabularies
    }
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyPropertyCompletions(task, fullUris, vocabularyFilter)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults, multiWordFilter = true).toJson)
  }

  @Operation(
    summary = "Mapping rule target types",
    description = "Given a search term, returns possible completions for target types.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All types from the target vocabulary that match the given term.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Completions])
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
  ))
  def targetTypes(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "task",
                    description = "The task identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  taskName: String,
                  @Parameter(
                    name = "rule",
                    description = "The rule identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  ruleName: String,
                  @Parameter(
                    name = "term",
                    description = "The search term. Will also return non-exact matches (e.g., naMe == name) and matches from labels.",
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String], defaultValue = "")
                  )
                  term: String,
                  @Parameter(
                    name = "maxResults",
                    description = "The maximum number of results.",
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[Int], defaultValue = "30")
                  )
                  maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val project = WorkspaceFactory().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyTypeCompletions(task)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults).toJson)
  }

  @Operation(
    summary = "Mapping rule value types",
    description = "Given a search term, returns possible completions for value types.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "All values types that match the given term.",
        content = Array(
          new Content(
            mediaType = "application/json",
            schema = new Schema(implementation = classOf[Completions]),
            examples = Array(new ExampleObject(AutoCompletionApiDoc.valueTypesExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
  ))
  def valueTypes(@Parameter(
                   name = "project",
                   description = "The project identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 projectName: String,
                 @Parameter(
                   name = "task",
                   description = "The task identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 taskName: String,
                 @Parameter(
                   name = "rule",
                   description = "The rule identifier",
                   required = true,
                   in = ParameterIn.PATH,
                   schema = new Schema(implementation = classOf[String])
                 )
                 ruleName: String,
                 @Parameter(
                   name = "term",
                   description = "The search term. Will also return non-exact matches (e.g., naMe == name) and matches from labels.",
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[String], defaultValue = "")
                 )
                 term: String,
                 @Parameter(
                   name = "maxResults",
                   description = "The maximum number of results.",
                   in = ParameterIn.QUERY,
                   schema = new Schema(implementation = classOf[Int], defaultValue = "30")
                 )
                 maxResults: Int): Action[AnyContent] = UserContextAction { implicit userContext =>
    val valueTypes = PluginRegistry.availablePlugins[ValueType].sortBy(_.label)
    val completions = Completions(valueTypes.map(valueTypeCompletion))
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

  def validateAutoCompletionRequest(autoCompletionRequest: AutoSuggestAutoCompletionRequest): Unit = {
    if(autoCompletionRequest.cursorPosition > autoCompletionRequest.inputString.length) {
      throw BadUserInputException("Cursor position must not be greater than the length of the input string!")
    }
    if(autoCompletionRequest.cursorPosition < 0) {
      throw BadUserInputException("Cursor position must not be negative.")
    }
    autoCompletionRequest.maxSuggestions foreach { maxSuggestions =>
      if(maxSuggestions <= 0) {
        throw BadUserInputException("Parameter 'maxSuggestions' must not be negative or zero!")
      }
    }
  }

  /** Validates a workflow context
    *
    * @param project             The project the workflow is in.
    * @param workflowTaskContext The workflow context that should be validated
    */
  def validateWorkflowContext(project: Project,
                              workflowTaskContext: Option[WorkflowTaskContext])
                             (implicit userContext: UserContext): InputAndOutputTasks = {
    var inputTasks: Seq[(WorkflowTaskContextInputTask, Task[_ <: TaskSpec])] = Seq.empty
    var outputTasks: Seq[(WorkflowTaskContextOutputTask, Task[_ <: TaskSpec])] = Seq.empty
    workflowTaskContext.foreach(c => {
      def checkTasks[T <: WorkflowTaskContextTask](tasks: Option[Seq[T]]): Seq[(T, Task[_ <: TaskSpec])] = {
        tasks match {
          case Some(ts) =>
            ts.map(t => (t, project.anyTask(t.id)))
          case None => Seq.empty
        }
      }
      inputTasks = checkTasks(c.inputTasks)
      outputTasks = checkTasks(c.outputTasks)
    })
    InputAndOutputTasks(inputTasks.map(t => InputTask(t._1, t._2)), outputTasks.map(t => OutputTask(t._1, t._2)))
  }

  case class InputTask(workflowContextTask: WorkflowTaskContextInputTask,
                       task: Task[_ <: TaskSpec])

  case class OutputTask(workflowContextTask: WorkflowTaskContextOutputTask,
                       task: Task[_ <: TaskSpec])

  private def mergeEntitySchemas(entitySchemas: Seq[EntitySchema]): EntitySchema = {
    val typedPaths = new ArrayBuffer[TypedPath]()
    entitySchemas.foreach(es => typedPaths.addAll(es.typedPaths))
    EntitySchema("", typedPaths.toIndexedSeq)
  }

  case class InputAndOutputTasks(inputTasks: Seq[InputTask],
                                 outputTasks: Seq[OutputTask]) {
    def inputTasksSchema(): Option[EntitySchema] = {
      if(inputTasks.isEmpty) {
        None
      } else {
        val entitySchemas = inputTasks.map(inputTask => {
          inputTask.task.data.outputPort match {
            case Some(FixedSchemaPort(schema)) => schema
            case _ => EntitySchema.empty
          }
        })
        Some(mergeEntitySchemas(entitySchemas))
      }
    }

    def outputTasksSchema(): Option[EntitySchema] = {
      if(outputTasks.isEmpty) {
        None
      } else {
        val entitySchemas = outputTasks.map(outputTask => {
          if(outputTask.workflowContextTask.configPort) {
            val pd = PluginDescription.forTask(outputTask.task)
            val parameters = pd.parameters.filter(_.visibleInDialog).map(_.name)
            EntitySchema("", parameters.map(p => UntypedPath.parse(p).asStringTypedPath).toIndexedSeq)
          } else {
            val port = outputTask.workflowContextTask.inputPort.get
            outputTask.task.data.inputPorts match {
              case FixedNumberOfInputs(ports) if port < ports.size =>
                ports(port).schemaOpt.getOrElse(EntitySchema.empty)
              case _ => EntitySchema.empty
            }
          }
        })
        Some(mergeEntitySchemas(entitySchemas))
      }
    }
  }
}