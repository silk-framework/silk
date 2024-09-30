package controllers.linking

import controllers.autoCompletion
import controllers.autoCompletion.{AutoSuggestAutoCompletionResponse, Completions}
import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.linking.linkingTask.LinkingTaskApiUtils
import controllers.shared.autoCompletion.AutoCompletionApiUtils
import controllers.transform.AutoCompletionApi
import controllers.transform.autoCompletion.{OpFilter, PartialSourcePathAutoCompletionRequest, PartialSourcePathAutocompletionHelper}
import controllers.transform.doc.AutoCompletionApiDoc
import controllers.transform.transformTask.TransformUtils
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.plugins.path.{PathMetaData, PathMetaDataPlugin, StandardMetaDataPlugin}
import org.silkframework.rule.{DatasetSelection, LinkSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.dataset.DatasetUtils
import org.silkframework.workspace.activity.linking.LinkingPathsCache
import org.silkframework.workspace.activity.transform.CachedEntitySchemata
import org.silkframework.workspace.{Project, ProjectTask}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

@Tag(name = "Linking")
class LinkingAutoCompletionApi @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {
  // All path meta data plugins
  private lazy val pathMetaDataPlugins: Map[Class[_], PathMetaDataPlugin[_]] = LinkingTaskApiUtils.pathMetaDataPlugins

  private def datasetPathMetaDataPlugin(datasetTask: ProjectTask[GenericDatasetSpec]): Option[PathMetaDataPlugin[Dataset]] = {
    pathMetaDataPlugins.get(datasetTask.data.plugin.getClass).map(_.asInstanceOf[PathMetaDataPlugin[Dataset]])
  }
  private val standardMetaDataPlugin = StandardMetaDataPlugin()

  @Operation(
    summary = "Linking task input paths completion",
    description = "Given a search term, returns all possible completions for the source or target input paths.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Input paths that match the given term",
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
        description = "If the specified project or task has not been found."
      )
    ))
  def linkingInputPaths(@Parameter(name = "project", description = "The project identifier", in = ParameterIn.PATH,
                          required = true,
                          schema = new Schema(implementation = classOf[String])
                        )
                        projectId: String,
                        @Parameter(
                          name = "task",
                          description = "The task identifier",
                          required = true,
                          in = ParameterIn.PATH,
                          schema = new Schema(implementation = classOf[String])
                        )
                        linkingTaskId: String,
                        @Parameter(
                          name = "target",
                          description = "If defined and set to true auto-completions for the target input source are returned, else for the source input source.",
                          required = true,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[Boolean])
                        )
                        targetPaths: Boolean,
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
                          name = "langPref",
                          description = "The preferred language of meta data like labels.",
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[String], defaultValue = "en")
                        )
                        langPref: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project, linkingTask) = projectAndTask[LinkSpec](projectId, linkingTaskId)
    val datasetSelection = if (targetPaths) linkingTask.target else linkingTask.source
    val entitySchemaOpt = linkingTask.activity[LinkingPathsCache].value.get.map(value => if(targetPaths) value.target else value.source)
    val cachedEntitySchemata = entitySchemaOpt.map(es => CachedEntitySchemata(es, None, linkingTask.id, None))
    val allPaths = AutoCompletionApiUtils.pathsCacheCompletions(datasetSelection.typeUri, cachedEntitySchemata, preferUntypedSchema = false,
      pathsMetaDataFactory(datasetSelection, langPref))
    // Return filtered result
    val filteredPaths = allPaths.filterAndSort(term, maxResults, multiWordFilter = true)
    Ok(filteredPaths.toJson)
  }

  // Creates a function that fetches the paths meta data
  private def pathsMetaDataFactory(datasetSelection: DatasetSelection,
                                   langPref: String)
                                  (implicit project: Project,
                                   userContext: UserContext): Option[Iterable[TypedPath] => Iterable[PathMetaData]] = {
    implicit val prefixes: Prefixes = project.config.prefixes
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId) match {
      case Some(dataset) =>
        datasetPathMetaDataPlugin(dataset).map(plugin=> {
          (paths: Iterable[TypedPath]) => plugin.fetchMetaData(dataset.data.plugin, paths, langPref)
        })
      case None =>
        None
    }
  }

  @Operation(
    summary = "Linking rule partial source paths",
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
                        linkingTaskId: String,
                        @Parameter(
                          name = "target",
                          description = "If defined and set to true, auto-completions for the target input source are returned, else for the source input source.",
                          required = true,
                          in = ParameterIn.QUERY,
                          schema = new Schema(implementation = classOf[Boolean])
                        )
                        isTarget: Boolean): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext =>
      val (project, linkingTask) = projectAndTask[LinkSpec](projectId, linkingTaskId)
      implicit val prefixes: Prefixes = project.config.prefixes
      validateJson[PartialSourcePathAutoCompletionRequest] { autoCompletionRequest =>
        AutoCompletionApi.validateAutoCompletionRequest(autoCompletionRequest)
        val datasetSelection = if (isTarget) linkingTask.target else linkingTask.source
        val autoCompletionResponse = autoCompletePartialSourcePath(linkingTask, datasetSelection, autoCompletionRequest, isTarget)
        Ok(Json.toJson(autoCompletionResponse))
      }
  }

  // Returns an auto-completion result for a partial path request
  private def autoCompletePartialSourcePath(linkingTask: ProjectTask[LinkSpec],
                                            datasetSelection: DatasetSelection,
                                            autoCompletionRequest: PartialSourcePathAutoCompletionRequest,
                                            isTarget: Boolean)
                                           (implicit userContext: UserContext): AutoSuggestAutoCompletionResponse = {
    implicit val project: Project = linkingTask.project
    implicit val prefixes: Prefixes = project.config.prefixes
    val isRdfInput = DatasetUtils.isRdfInput(project, datasetSelection)
    val pathToReplace = PartialSourcePathAutocompletionHelper.pathToReplace(autoCompletionRequest, isRdfInput)
    val dataSourceCharacteristicsOpt = DatasetUtils.datasetCharacteristics(project, datasetSelection)
    val supportsAsteriskOperator = dataSourceCharacteristicsOpt.exists(_.supportsAsteriskPathOperator)
    // compute relative paths
    val pathBeforeReplacement = UntypedPath.partialParse(autoCompletionRequest.inputString.take(pathToReplace.from)).partialPath
    val completeSubPath = pathBeforeReplacement.operators
    val simpleSubPath = AutoCompletionApiUtils.simplePath(completeSubPath)
    val forwardOnlySubPath = AutoCompletionApiUtils.forwardOnlyPath(simpleSubPath)
    val linkingPathsCache = linkingTask.activity[LinkingPathsCache]
    val entitySchemaOpt = linkingPathsCache.value.get.map(cacheValue => if(isTarget) cacheValue.target else cacheValue.source)
    val cachedEntitySchemata = entitySchemaOpt.map(entitySchema => CachedEntitySchemata(entitySchema, if(isRdfInput) Some(entitySchema) else None, datasetSelection.inputId, None))
    val allPaths = AutoCompletionApiUtils.pathsCacheCompletions(datasetSelection.typeUri, cachedEntitySchemata, simpleSubPath.nonEmpty && isRdfInput)
    val pathOpFilter = (autoCompletionRequest.isInBackwardOp, autoCompletionRequest.isInExplicitForwardOp) match {
      case (true, false) => OpFilter.Backward
      case (false, true) => OpFilter.Forward
      case _ => OpFilter.None
    }
    val relativePaths = AutoCompletionApiUtils.extractRelativePaths(simpleSubPath, forwardOnlySubPath, allPaths, isRdfInput, oneHopOnly = pathToReplace.insideFilter,
      serializeFull = !pathToReplace.insideFilter && pathToReplace.from > 0, pathOpFilter = pathOpFilter,
      supportsAsteriskOperator = supportsAsteriskOperator
    )
    val dataSourceSpecialPathCompletions = PartialSourcePathAutocompletionHelper.specialPathCompletions(dataSourceCharacteristicsOpt, pathToReplace, pathOpFilter, isObjectPath = false)
    // Add known paths
    val completions = autoCompletion.Completions(relativePaths ++ dataSourceSpecialPathCompletions)
    val operatorCompletions = PartialSourcePathAutocompletionHelper.operatorCompletions(dataSourceCharacteristicsOpt, pathToReplace, autoCompletionRequest)
    // Return filtered result
    val filteredResults = PartialSourcePathAutocompletionHelper.filterResults(autoCompletionRequest, pathToReplace, completions,
      filterOutSingleExactQueryStringCompletion = operatorCompletions.forall(_.replacements.isEmpty))
    AutoCompletionApiUtils.partialAutoCompletionResult(autoCompletionRequest, pathToReplace, operatorCompletions, filteredResults)
  }
}
