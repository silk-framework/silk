package org.silkframework.workspace.activity.linking

import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "LinkingPathsCache",
  label = "Linking Paths Cache",
  categories = Array("LinkSpecification"),
  description = "Holds the most frequent paths for the selected entities."
)
case class LinkingPathsCacheFactory() extends TaskActivityFactory[LinkSpec, LinkingPathsCache] {

  override def autoRun = true

  def apply(task: ProjectTask[LinkSpec]): Activity[DPair[EntitySchema]] = {
    new CachedActivity(
      activity = new LinkingPathsCache(task),
      resource = task.project.cacheResources.child("linking").child(task.id).get(s"pathsCache.xml")
    )
  }
}
