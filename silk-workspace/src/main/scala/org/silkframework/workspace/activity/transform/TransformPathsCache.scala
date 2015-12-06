package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.activity.{ActivityContext, Activity}
import org.silkframework.workspace.Project
import org.silkframework.workspace.Task

import scala.xml.Node

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(task: Task[TransformSpecification]) extends Activity[SparqlEntitySchema] {

  override def name = s"Paths cache ${task.name}"

  override def initialValue = Some(SparqlEntitySchema.empty)

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[SparqlEntitySchema]) = {
    val dataset = task.project.task[Dataset](task.data.selection.datasetId).data
    val transform = task.data

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.entityDescription

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value().isEmpty || currentEntityDesc.restrictions != context.value().restrictions) {
      // Retrieve the data sources
      val source = dataset.source
      //Retrieve most frequent paths
      context.status.update("Retrieving frequent paths", 0.0)
      val paths = source.retrieveSparqlPaths(transform.selection.restriction, 1).map(_._1)
      //Add the frequent paths to the entity description
      context.value() = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ paths).distinct)
    }
  }
}