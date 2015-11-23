package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.Dataset
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.activity.Activity
import org.silkframework.util.DPair
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

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
