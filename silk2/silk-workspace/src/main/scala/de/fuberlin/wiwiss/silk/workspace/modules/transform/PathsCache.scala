package de.fuberlin.wiwiss.silk.workspace.modules.transform

import de.fuberlin.wiwiss.silk.workspace.modules.Cache
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.workspace.Project
import scala.xml.{Node, NodeSeq}

/**
 * Holds the most frequent paths.
 */
class PathsCache() extends Cache[TransformTask, EntityDescription](null) {

  /**
   * Loads the most frequent paths.
   */
  override def update(project: Project, task: TransformTask) {
    updateStatus("Retrieving frequent paths", 0.0)

    //Create an entity description from the transformation task
    val currentEntityDesc = task.entityDescription

    //Check if the restriction has been changed
    val update = value == null || currentEntityDesc.restrictions != value.restrictions

    if (value == null || update) {
      // Retrieve the data sources
      val source = project.sourceModule.task(task.dataset.sourceId).source

      //Retrieve most frequent paths
      val paths = source.retrievePaths(task.dataset.restriction, 1, Some(50))

      //Add the frequent paths to the entity description
      value = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ paths.map(_._1)).distinct)
    } else {
      //Add the existing paths to the entity description
      value = currentEntityDesc.copy(paths = (currentEntityDesc.paths ++ value.paths).distinct)
    }
  }

  override def toXML: NodeSeq = {
    if (value != null) {
      <EntityDescription>
        {value.toXML}
      </EntityDescription>
    } else {
      NodeSeq.fromSeq(Nil)
    }
  }

  override def loadFromXML(node: Node) {
    value =
      if ((node \ "EntityDescription").isEmpty) {
        null
      } else {
        EntityDescription.fromXML(node \ "EntityDescription" \ "_" head)
      }
  }
}