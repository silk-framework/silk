package org.silkframework.workspace.activity.linking

import org.silkframework.entity.EntitySchema
import org.silkframework.rule.{DatasetSelection, LinkSpec, TransformSpec}
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

  // Is true if the data of an input source has changed
  private var sourceChanged: Boolean = false

  private val transformSpecObserverFunctions: (TransformSpec) => Unit = {
    val fn: (TransformSpec) => Unit = (_) => {
      sourceChanged = true
      task.activity[LinkingPathsCache].start()
    }
    for(selection <- task.data.dataSelections) {
      val sourceInputId = selection.inputId
      // Only do this automatically if the input is a transformation
      task.project.taskOption[TransformSpec](sourceInputId).foreach { inputTask =>
        inputTask.dataValueHolder.subscribe(fn)
      }
    }
    fn
  }

  /**
   * Loads the most frequent property paths.
   */
  override def run(context: ActivityContext[DPair[EntitySchema]]): Unit = {
    context.status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val typeChanged = currentEntityDescs.source.typeUri != context.value().source.typeUri &&
        currentEntityDescs.target.typeUri != context.value().target.typeUri
    val emptyPaths = context.value().source.typedPaths.isEmpty && context.value().target.typedPaths.isEmpty
    val update = emptyPaths ||
        typeChanged ||
        sourceChanged

    // Update paths
    if (update) {
      val updatedSchemata =
        for((dataSelection, entitySchema) <- linkSpec.dataSelections zip currentEntityDescs) yield {
          if(sourceChanged && !(emptyPaths || typeChanged)) {
            // Only transformation sources changed
            if(task.project.taskOption[TransformSpec](dataSelection.inputId).isDefined) {
              updateSchema(dataSelection, entitySchema)
            } else {
              entitySchema // Do not update other source schemata
            }
          } else {
            updateSchema(dataSelection, entitySchema)
          }
        }
      context.value.update(updatedSchemata)
    }
  }

  private def updateSchema(datasetSelection: DatasetSelection,
                           entitySchema: EntitySchema): EntitySchema = {
    val paths = retrievePaths(datasetSelection)
    entitySchema.copy(typedPaths = (entitySchema.typedPaths ++ paths.map(_.asStringTypedPath)).distinct)
  }

  private def retrievePaths(datasetSelection: DatasetSelection) = {
    // Retrieve the data source
    val source = task.dataSource(datasetSelection)
    // Retrieve most frequent paths
    source.retrievePaths(datasetSelection.typeUri, 1, Some(50))
  }
}
