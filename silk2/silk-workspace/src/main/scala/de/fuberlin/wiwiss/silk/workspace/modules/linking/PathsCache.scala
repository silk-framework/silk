package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.workspace.Project
import xml.{Node, NodeSeq, NodeBuffer}

/**
 * Holds the most frequent paths.
 */
class PathsCache() extends Cache[DPair[EntityDescription]](null) {

  /**
   * Loads the most frequent property paths.
   */
  override def update(project: Project, task: LinkingTask) {
    updateStatus("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = task.linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val update =
      value == null ||
      currentEntityDescs.source.restrictions != value.source.restrictions &&
      currentEntityDescs.target.restrictions != value.target.restrictions

    if (value == null || update) {
      // Retrieve the data sources
      val sources = task.linkSpec.datasets.map(ds => project.sourceModule.task(ds.sourceId).source)

      //Retrieve most frequent paths
      val paths = for ((source, dataset) <- sources zip task.linkSpec.datasets) yield source.retrievePaths(dataset.restriction, 1, Some(50))

      //Add the frequent paths to the entity description
      value = for ((entityDesc, paths) <- currentEntityDescs zip paths) yield entityDesc.copy(paths = (entityDesc.paths ++ paths.map(_._1)).distinct)
    } else {
      //Add the existing paths to the entity description
      value = for ((spec1, spec2) <- currentEntityDescs zip value) yield spec1.copy(paths = (spec1.paths ++ spec2.paths).distinct)
    }
  }

  override def toXML: NodeSeq = {
    if (value != null) {
        <EntityDescriptions>
          <Source>
            {value.source.toXML}
          </Source>
          <Target>
            {value.target.toXML}
          </Target>
        </EntityDescriptions>
    } else {
      NodeSeq.fromSeq(Nil)
    }
  }

  override def loadFromXML(node: Node) {
    value =
      if ((node \ "EntityDescriptions").isEmpty) {
        null
      } else {
        val sourceSpec = EntityDescription.fromXML(node \ "EntityDescriptions" \ "Source" \ "_" head)
        val targetSpec = EntityDescription.fromXML(node \ "EntityDescriptions" \ "Target" \ "_" head)
        new DPair(sourceSpec, targetSpec)
      }
  }
}
