package controllers.linking

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.linking.linkingTask.LinkingTaskApiUtils
import controllers.shared.autoCompletion.AutoCompletionApiUtils
import controllers.transform.autoCompletion.Completions
import controllers.transform.doc.AutoCompletionApiDoc
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.silkframework.config.Prefixes
import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.entity.paths.TypedPath
import org.silkframework.plugins.path.{PathMetaData, PathMetaDataPlugin, StandardMetaDataPlugin}
import org.silkframework.rule.{DatasetSelection, LinkSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.{Project, ProjectTask}
import org.silkframework.workspace.activity.linking.LinkingPathsCache
import org.silkframework.workspace.activity.transform.CachedEntitySchemata
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

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
    val allPaths = AutoCompletionApiUtils.pathsCacheCompletions(datasetSelection, cachedEntitySchemata, preferUntypedSchema = false,
      pathsMetaDataFactory(datasetSelection, langPref))
    // Return filtered result
    val filteredPaths = allPaths.filterAndSort(term, maxResults, sortEmptyTermResult = false)
    Ok(filteredPaths.toJson)
  }

  // Creates a function that fetches the paths meta data
  private def pathsMetaDataFactory(datasetSelection: DatasetSelection,
                                   langPref: String)
                                  (implicit project: Project,
                                   userContext: UserContext): Option[Traversable[TypedPath] => Traversable[PathMetaData]] = {
    implicit val prefixes: Prefixes = project.config.prefixes
    project.taskOption[GenericDatasetSpec](datasetSelection.inputId) match {
      case Some(dataset) =>
        datasetPathMetaDataPlugin(dataset).map(plugin=> {
          (paths: Traversable[TypedPath]) => plugin.fetchMetaData(dataset.data.plugin, paths, langPref)
        })
      case None =>
        None
    }
  }
}
