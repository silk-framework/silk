package de.fuberlin.wiwiss.silk.workspace.activity.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.activity.{CachedActivity, TaskActivityFactory}
import de.fuberlin.wiwiss.silk.workspace.{Task}

class LinkingPathsCacheFactory extends TaskActivityFactory[LinkSpecification, LinkingPathsCache, DPair[SparqlEntitySchema]] {

  override def autoRun = true

  def apply(task: Task[LinkSpecification]): Activity[DPair[SparqlEntitySchema]] = {
    new CachedActivity(
      activity =
        new LinkingPathsCache(
          datasets = task.data.dataSelections.map(ds => task.project.task[Dataset](ds.datasetId).data),
          linkSpec = task.data
        ),
      resource = task.project.cacheResources.child("linking").child(task.name).get(s"pathsCache.xml")
    )
  }
}
