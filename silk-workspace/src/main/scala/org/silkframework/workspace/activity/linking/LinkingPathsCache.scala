package org.silkframework.workspace.activity.linking

import org.silkframework.entity.EntitySchema
import org.silkframework.rule.{DatasetSelection, LinkSpec}
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.PathsCacheTrait
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._

/**
 * Holds the most frequent paths.
 */
class LinkingPathsCache(task: ProjectTask[LinkSpec]) extends Activity[DPair[EntitySchema]] with PathsCacheTrait {

  private def linkSpec = task.data

  override def name: String = s"Paths cache ${task.id}"

  override def initialValue: Option[DPair[EntitySchema]] = Some(DPair.fill(EntitySchema.empty))

  /**
   * Loads the most frequent property paths.
   */
  override def run(context: ActivityContext[DPair[EntitySchema]]): Unit = {
    context.status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val update =
      (context.value().source.typedPaths.isEmpty && context.value().target.typedPaths.isEmpty) ||
      (currentEntityDescs.source.typeUri != context.value().source.typeUri &&
       currentEntityDescs.target.typeUri != context.value().target.typeUri)

    // Update paths
    if (update) {
      val updatedSchemata =
        for((dataSelection, entitySchema) <- linkSpec.dataSelections zip currentEntityDescs) yield {
          val paths = retrievePaths(dataSelection)
          entitySchema.copy(typedPaths = (entitySchema.typedPaths ++ paths.map(_.asStringTypedPath)).distinct)
        }
      context.value.update(updatedSchemata)
    }
  }

  private def retrievePaths(datasetSelection: DatasetSelection) = {
    // Retrieve the data source
    val source = task.dataSource(datasetSelection.inputId)
    // Retrieve most frequent paths
    source.retrievePaths(datasetSelection.typeUri, 1, Some(50))
  }
}
