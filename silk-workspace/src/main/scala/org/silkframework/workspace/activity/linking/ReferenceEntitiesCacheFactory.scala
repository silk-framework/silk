package org.silkframework.workspace.activity.linking

import org.silkframework.config.LinkSpecification
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "referenceEntitiesCache",
  label = "Reference Entities Cache",
  categories = Array("LinkSpecification"),
  description = "For each reference link, the reference entities cache holds all values of the linked entities."
)
case class ReferenceEntitiesCacheFactory() extends TaskActivityFactory[LinkSpecification, ReferenceEntitiesCache] {

  override def autoRun = true

  def apply(task: Task[LinkSpecification]): Activity[ReferenceEntities] = {
    new CachedActivity(
      activity = new ReferenceEntitiesCache(task),
      resource = task.project.cacheResources.child("linking").child(task.name).get(s"referenceEntitiesCache.xml")
    )
  }
}
