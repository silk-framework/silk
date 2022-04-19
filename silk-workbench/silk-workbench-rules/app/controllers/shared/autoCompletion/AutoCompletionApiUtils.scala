package controllers.shared.autoCompletion

import controllers.transform.AutoCompletionApi.Categories
import controllers.transform.autoCompletion.{Completion, Completions}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.TypedPath
import org.silkframework.plugins.path.PathMetaData
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.transform.CachedEntitySchemata

import scala.collection.mutable

object AutoCompletionApiUtils {
  /** Gets the paths cache value as auto-autocompletion objects. */
  def pathsCacheCompletions(datasetSelection: DatasetSelection,
                            cachedEntitySchema: Option[CachedEntitySchemata],
                            preferUntypedSchema: Boolean,
                            pathMetaData: Option[Traversable[TypedPath] => Traversable[PathMetaData]] = None)
                           (implicit userContext: UserContext,
                            project: Project): Completions = {
    implicit val prefixes: Prefixes = project.config.prefixes
    if (cachedEntitySchema.isDefined) {
      val paths = fetchCachedPaths(datasetSelection, cachedEntitySchema.get, preferUntypedSchema)
      val serializedPathSet = new mutable.HashSet[String]()
      val serializedPaths = paths
        // Sort primarily by path operator length then name
        .sortWith { (p1, p2) =>
          if (p1.operators.length == p2.operators.length) {
            p1.serialize() < p2.serialize()
          } else {
            p1.operators.length < p2.operators.length
          }
        }
        .map(p => (p.toUntypedPath.serialize()(prefixes), p))
        .filter(p => {
          val passes = !serializedPathSet.contains(p._1)
          serializedPathSet.add(p._1)
          passes
        })
      val pathMetaDataMap: Map[String, String] = pathMetaData match {
        case Some(fetchPathMetaData) =>
          val pathsMetaData = fetchPathMetaData(serializedPaths.map(_._2))
          pathsMetaData
            .filter(_.label.isDefined)
            .map(p => (p.value, p.label.get))
            .toMap
        case None =>
          Map.empty
      }
      val completions = for((pathStr, _) <- serializedPaths) yield {
        Completion(
          value = pathStr,
          label = pathMetaDataMap.get(pathStr),
          description = None,
          category = Categories.sourcePaths,
          isCompletion = true
        )
      }
      Completions(completions)
    } else {
      Completions()
    }
  }

  private def fetchCachedPaths(datasetSelection: DatasetSelection,
                               cachedEntitySchema: CachedEntitySchemata,
                               preferUntypedSchema: Boolean)
                              (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    cachedEntitySchema.fetchCachedPaths(datasetSelection, preferUntypedSchema)
  }
}
