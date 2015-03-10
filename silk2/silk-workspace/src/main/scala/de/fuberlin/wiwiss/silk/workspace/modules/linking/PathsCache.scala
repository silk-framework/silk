package de.fuberlin.wiwiss.silk.workspace.modules.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

/**
 * Holds the most frequent paths.
 */
class PathsCache() extends Cache[LinkSpecification, DPair[EntityDescription]](null) {

  def entityDescs = Option(value).getOrElse(DPair.fill(EntityDescription.empty))

  /**
   * Loads the most frequent property paths.
   */
  override def update(project: Project, linkSpec: LinkSpecification) = {
    status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val update =
      value == null ||
      currentEntityDescs.source.restrictions != value.source.restrictions &&
      currentEntityDescs.target.restrictions != value.target.restrictions

    if (value == null || update) {
      // Retrieve the data sources
      val sources = linkSpec.datasets.map(ds => project.task[Dataset](ds.datasetId).data.source)

      //Retrieve most frequent paths
      val paths = for ((source, dataset) <- sources zip linkSpec.datasets) yield source.retrievePaths(dataset.restriction, 1, Some(50))

      //Add the frequent paths to the entity description
      value = for ((entityDesc, paths) <- currentEntityDescs zip paths) yield entityDesc.copy(paths = (entityDesc.paths ++ paths.map(_._1)).distinct)
      true
    } else {
      //Add the existing paths to the entity description
      value = for ((spec1, spec2) <- currentEntityDescs zip value) yield spec1.copy(paths = (spec1.paths ++ spec2.paths).distinct)
      false
    }
  }

  override def serialize: Node = {
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
      <EntityDescriptions>
      </EntityDescriptions>
    }
  }

  override def deserialize(node: Node) {
    value =
      if ((node \ "_").isEmpty) {
        null
      } else {
        val sourceSpec = EntityDescription.fromXML((node \ "Source" \ "_").head)
        val targetSpec = EntityDescription.fromXML((node \ "Target" \ "_").head)
        new DPair(sourceSpec, targetSpec)
      }
  }
}
