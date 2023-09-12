package controllers.transform

import controllers.core.UserContextActions
import controllers.transform.doc.SourceTaskApiDoc
import controllers.transform.transformTask.{ObjectValueSourcePathInfo, TransformUtils, ValueSourcePathInfo}
import controllers.util.ProjectUtils.getProjectAndTask
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{PathOperator, TypedPath, UntypedPath}
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.workspace.{Project, ProjectTask}
import org.silkframework.workspace.activity.transform.TransformPathsCache
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

@Tag(name = "Transform")
class SourcePathsApi @Inject() () extends InjectedController with UserContextActions {

  @Operation(
    summary = "Mapping Rule Value Source Paths",
    description = "Returns an array of string representations of the available source paths.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of source paths serialized with prefixed URIs.",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(SourceTaskApiDoc.valueSourcePathsExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def valueSourcePaths(@Parameter(
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
                       ruleId: String,
                       @Parameter(
                         name = "maxDepth",
                         description = "Limit the depth of the source paths. For example a value of 1 would only return value source paths with exactly one path operator.",
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[Int])
                       )
                       maxDepth: Int,
                       @Parameter(
                         name = "unusedOnly",
                         description = "If this is set to true, only source paths that are not used in any rule so far are returned. Considered rules for filtering are only value rules and complex mappings with a single source path.",
                         required = false,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                       )
                       unusedOnly: Boolean,
                       @Parameter(
                        name = "usedOnly",
                        description = "If this is set to true, only source paths that are already used in any rule so far are returned. Considered rules for filtering are only value rules and complex mappings with a single source path. This must not be true if `unusedOnly` is true.",
                        required = false,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                      )
                      usedOnly: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    if(unusedOnly && usedOnly) {
      throw BadUserInputException("Only one of the following parameters can be true, but both of them were true: unusedOnly, usedOnly")
    }
    implicit val (project: Project, task: ProjectTask[TransformSpec]) = getProjectAndTask[TransformSpec](projectName, taskName)
    implicit val prefixes: Prefixes = project.config.prefixes
    Ok(Json.toJson(typedRuleValuePaths(task, ruleId, maxDepth, unusedOnly, usedOnly).map(_.serialize())))
  }

  @Operation(
    summary = "Mapping Rule Value Source Paths Information",
    description = "Fetch all value source paths relative to the corresponding rule. Besides the path string that is given in the Silk path language format, additional information about the source path is returned.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "A list of source paths with additional information.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ValueSourcePathInfo]),
          examples = Array(new ExampleObject(SourceTaskApiDoc.valueSourcePathInfoExample))
        ))
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or rule has not been found."
      )
    )
  )
  def valueSourcePathsFullInfo(@Parameter(
                                 name = "project",
                                 description = "The project identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
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
                               transformTaskId: String,
                               @Parameter(
                                 name = "rule",
                                 description = "The rule identifier",
                                 required = true,
                                 in = ParameterIn.PATH,
                                 schema = new Schema(implementation = classOf[String])
                               )
                               mappingRuleId: String,
                               @Parameter(
                                 name = "maxDepth",
                                 description = "Limit the depth of the source paths. For example a value of 1 would only return value source paths with exactly one path operator.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[Int])
                               )
                               maxDepth: Int,
                               @Parameter(
                                 name = "objectInfo",
                                 description = "If it should return additional information for object paths.",
                                 required = false,
                                 in = ParameterIn.QUERY,
                                 schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                               )
                               objectInfo: Boolean): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project: Project, transformTask: ProjectTask[TransformSpec]) = getProjectAndTask[TransformSpec](projectId, transformTaskId)
    implicit val prefixes: Prefixes = project.config.prefixes
    val dataSourceCharacteristicsOpt = TransformUtils.datasetCharacteristics(transformTask)
    val typedPaths = typedRuleValuePaths(transformTask, mappingRuleId, maxDepth)
    val usedPaths = typedRuleValuePaths(transformTask, mappingRuleId, maxDepth, usedOnly = true).toSet
    val objectPathInfos: Map[UntypedPath, ObjectValueSourcePathInfo] = dataSourceCharacteristicsOpt match {
      case Some(characteristics) if characteristics.supportedPathExpressions.multiHopPaths && objectInfo =>
        val objectPaths = typedPaths.filter(_.valueType.id == "UriValueType").map(_.toUntypedPath).toSet
        val isRdfInput = TransformUtils.isRdfInput(transformTask)
        if(objectPaths.isEmpty) {
          Map.empty
        } else if (isRdfInput) {
          // TODO: Fetch RDF infos
          Map.empty
        } else {
          val maxObjectPathLength = objectPaths.map(_.operators.size).max
          val additionalHopPath = if(maxDepth < Int.MaxValue || maxObjectPathLength == maxDepth) {
            // Need to fetch paths with one more hop in order to calculate stats
            typedRuleValuePaths(transformTask, mappingRuleId, maxDepth + 1)
          } else {
            typedPaths
          }
          val subPathMap = mutable.HashMap[UntypedPath, ArrayBuffer[TypedPath]]()
          additionalHopPath filter { tp => tp.operators.size > 1 } foreach { tp =>
            val parentPath = UntypedPath(tp.operators.dropRight(1))
            if(objectPaths.contains(parentPath)) {
              subPathMap.getOrElseUpdate(parentPath, new ArrayBuffer[TypedPath]()).append(tp)
            }
          }
          def relativePathString(tp: TypedPath) = UntypedPath(tp.operators.takeRight(1)).serialize()
          subPathMap.view.mapValues { typedPaths =>
            val (objectPaths, dataPaths) = typedPaths.partition(_.valueType.id == "UriValueType")
            ObjectValueSourcePathInfo(
              dataTypeSubPaths = dataPaths.map(relativePathString).toSeq,
              objectSubPaths = objectPaths.map(relativePathString).toSeq
            )
          }.toMap
        }
      case _ => Map.empty
    }
    val valuePathInfos = typedPaths map { tp =>
      val pathType = if(tp.valueType.id == "UriValueType") "object" else "value"
      ValueSourcePathInfo(tp.serialize(), pathType, alreadyMapped = usedPaths.contains(tp), objectPathInfos.get(tp.toUntypedPath))
    }
    Ok(Json.toJson(valuePathInfos))
  }

  private def typedRuleValuePaths(task: ProjectTask[TransformSpec],
                                  ruleId: String,
                                  maxDepth: Int,
                                  unusedOnly: Boolean = false,
                                  usedOnly: Boolean = false)
                                 (implicit userContext: UserContext,
                                  prefixes: Prefixes): IndexedSeq[TypedPath] = {
    assert(!unusedOnly || !usedOnly, "Only one of the following parameters can be true, but both of them were true: unusedOnly, usedOnly")
    task.nestedRuleAndSourcePath(ruleId) match {
      case Some((_, sourcePath)) =>
        val matchingPaths = relativePathsFromPathsCache(maxDepth, sourcePath, task)
        if(unusedOnly || usedOnly) {
          val sourcePaths = usedSourcePaths(ruleId, maxDepth, task)
          val filterFn: UntypedPath => Boolean = if(unusedOnly) path => !sourcePaths.contains(path) else sourcePaths.contains
          matchingPaths filter { path =>
            filterFn(path.asUntypedPath)
          }
        } else {
          matchingPaths
        }
      case None =>
        throw NotFoundException("No rule found with ID " + ruleId)
    }
  }

  // Get the relative paths matching the path prefix with the max. depth length from the transform path cache.
  private def relativePathsFromPathsCache(maxDepth: Int,
                                          pathPrefix: List[PathOperator],
                                          task: ProjectTask[TransformSpec])
                                         (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val pathCache = task.activity[TransformPathsCache]
    pathCache.control.waitUntilFinished()
    val isRdfInput = TransformUtils.isRdfInput(task)
    val cachedPaths = pathCache.value().fetchCachedPaths(task.selection, pathPrefix.nonEmpty && isRdfInput)
    cachedPaths filter { p =>
      val pathSize = p.operators.size
      isRdfInput ||
        p.operators.startsWith(pathPrefix) &&
          pathSize > pathPrefix.size &&
          pathSize - pathPrefix.size <= maxDepth
    } map { p =>
      if (isRdfInput) {
        p
      } else {
        TypedPath.removePathPrefix(p, UntypedPath(pathPrefix))
      }
    }
  }

  /** The relative source path that are already used in the specified mapping rule. */
  private def usedSourcePaths(ruleId: String, maxDepth: Int,
                              task: ProjectTask[TransformSpec]): Set[UntypedPath] = {
    task.data.valueSourcePaths(ruleId, maxDepth).toSet ++
      task.data.objectSourcePaths(ruleId).toSet
  }

}
