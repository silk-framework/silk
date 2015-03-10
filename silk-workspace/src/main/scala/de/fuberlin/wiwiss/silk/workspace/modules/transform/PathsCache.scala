package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

/**
 * Holds the most frequent paths.
 */
class PathsCache() extends Cache[TransformSpecification, EntityDescription](null) {

  /**
   * Loads the most frequent paths.
   */
  override def update(project: Project, task: TransformSpecification) = {
    status.update("Retrieving frequent paths", 0.0)

    //Create an entity description from the transformation task
    val currentEntityDesc = task.entityDescription

    //Check if paths have not been loaded yet or if the restriction has been changed
    if (value == null || currentEntityDesc.restrictions != value.restrictions) {
      // Retrieve the data sources
      val source = project.task[Dataset](task.selection.datasetId).data.source

      //Retrieve most frequent paths
      val paths = source.retrievePaths(task.selection.restriction, 1).map(_._1)

      //Add the frequent paths to the entity description
      value = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ paths).distinct)
      true
    } else {
      //Add the existing paths to the entity description
      value = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ value.paths).distinct)
      false
    }
  }

  override def serialize: Node = {
    if (value != null) {
      value.toXML
    } else {
      <EntityDescription>
      </EntityDescription>
    }
  }

  override def deserialize(node: Node) {
    value =
      if ((node \ "_").isEmpty) {
        null
      } else {
        EntityDescription.fromXML(node)
      }
  }
}