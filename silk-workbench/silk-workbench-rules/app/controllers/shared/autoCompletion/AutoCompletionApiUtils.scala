package controllers.shared.autoCompletion

import controllers.transform.AutoCompletionApi.Categories
import controllers.transform.autoCompletion.{Completion, Completions}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.TypedPath
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.transform.CachedEntitySchemata

object AutoCompletionApiUtils {
  /** Gets the paths cache value as auto-autocompletion objects. */
  def pathsCacheCompletions(datasetSelection: DatasetSelection,
                            cachedEntitySchema: Option[CachedEntitySchemata],
                            preferUntypedSchema: Boolean)
                           (implicit userContext: UserContext,
                            prefixes: Prefixes): Completions = {
    if (cachedEntitySchema.isDefined) {
      val paths = fetchCachedPaths(datasetSelection, cachedEntitySchema.get, preferUntypedSchema)
      val serializedPaths = paths
        // Sort primarily by path operator length then name
        .sortWith { (p1, p2) =>
          if (p1.operators.length == p2.operators.length) {
            p1.serialize() < p2.serialize()
          } else {
            p1.operators.length < p2.operators.length
          }
        }
        .map(_.toUntypedPath.serialize()(prefixes))
        .distinct
      val completions = for(pathStr <- serializedPaths) yield {
        Completion(
          value = pathStr,
          label = None,
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
