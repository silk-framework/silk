package de.fuberlin.wiwiss.silk.workspace.activity.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.workspace.{Task, Project}

import scala.xml.Node

/**
 * Holds the most frequent paths.
 */
class TransformPathsCache(task: Task[TransformSpecification]) extends Activity[SparqlEntitySchema] {

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[SparqlEntitySchema]) = {
    val dataset = task.project.task[Dataset](task.data.selection.datasetId).data
    val transform = task.data

    //Create an entity description from the transformation task
    val currentEntityDesc = transform.entityDescription

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value() == null || currentEntityDesc.restrictions != context.value().restrictions) {
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