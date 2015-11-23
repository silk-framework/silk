package de.fuberlin.wiwiss.silk.workspace.activity.linking

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.activity.{CachedActivity, TaskActivityFactory}
import de.fuberlin.wiwiss.silk.workspace.{Task}

class ReferenceEntitiesCacheFactory extends TaskActivityFactory[LinkSpecification, ReferenceEntitiesCache, ReferenceEntities] {

  override def autoRun = true

  def apply(task: Task[LinkSpecification]): Activity[ReferenceEntities] = {
    new CachedActivity(
      activity = new ReferenceEntitiesCache(task),
      resource = task.project.cacheResources.child("linking").child(task.name).get(s"referenceEntitiesCache.xml")
    )
  }
}
