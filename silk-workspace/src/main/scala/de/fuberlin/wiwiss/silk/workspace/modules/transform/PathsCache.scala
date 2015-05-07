package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

/**
 * Holds the most frequent paths.
 */
class PathsCache(dataset: Dataset, transform: TransformSpecification) extends Activity[EntityDescription] {

  /**
   * Loads the most frequent paths.
   */
  override def run(context: ActivityContext[EntityDescription]) = {
    //Create an entity description from the transformation task
    val currentEntityDesc = transform.entityDescription

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (context.value() == null || currentEntityDesc.restrictions != context.value().restrictions) {
      // Retrieve the data sources
      val source = dataset.source
      //Retrieve most frequent paths
      context.status.update("Retrieving frequent paths", 0.0)
      val paths = source.retrievePaths(transform.selection.restriction, 1).map(_._1)
      //Add the frequent paths to the entity description
      context.value() = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ paths).distinct)
    }
  }
}