package org.silkframework.workspace.activity.linking

import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "ReferenceEntitiesCache",
  label = "Reference Entities Cache",
  categories = Array("LinkSpecification"),
  description = "For each reference link, the reference entities cache holds all values of the linked entities."
)
case class ReferenceEntitiesCacheFactory() extends TaskActivityFactory[LinkSpec, ReferenceEntitiesCache] {

  override def autoRun = true

  def apply(task: ProjectTask[LinkSpec]): Activity[ReferenceEntities] = {
    new CachedActivity(
      activity = new ReferenceEntitiesCache(task),
      resource = task.project.cacheResources.child("linking").child(task.id).get(s"referenceEntitiesCache.xml")
    )
  }
}
