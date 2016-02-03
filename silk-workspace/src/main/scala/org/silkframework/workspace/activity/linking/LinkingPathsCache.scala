package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.DPair

/**
 * Holds the most frequent paths.
 */
class LinkingPathsCache(datasets: DPair[Dataset], linkSpec: LinkSpecification) extends Activity[DPair[EntitySchema]] {

  override def name = s"Paths cache ${linkSpec.id}"

  override def initialValue = Some(DPair.fill(EntitySchema.empty))

  /**
   * Loads the most frequent property paths.
   */
  override def run(context: ActivityContext[DPair[EntitySchema]]) = {
    context.status.update("Retrieving frequent property paths", 0.0)

    //Create an entity description from the link specification
    val currentEntityDescs = linkSpec.entityDescriptions

    //Check if the restriction has been changed
    val update =
      (context.value().source.paths.isEmpty && context.value().target.paths.isEmpty) ||
      (currentEntityDescs.source.typeUri != context.value().source.typeUri &&
       currentEntityDescs.target.typeUri != context.value().target.typeUri)

    if (update) {
      // Retrieve the data sources
      val sources = datasets.map(_.source)
      //Retrieve most frequent paths
      val paths = for ((source, dataset) <- sources zip linkSpec.dataSelections) yield source.retrievePaths(dataset.typeUri, 1, Some(50))
      //Add the frequent paths to the entity description
      context.value() = for ((entityDesc, paths) <- currentEntityDescs zip paths) yield entityDesc.copy(paths = entityDesc.paths ++ paths.distinct)
    }
  }
}
