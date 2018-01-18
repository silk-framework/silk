package org.silkframework.workspace.activity.dataset

import org.silkframework.dataset.{DatasetSpec, Dataset}
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "TypesCache",
  label = "Types Cache",
  categories = Array("Dataset"),
  description = "Holds the most frequent types in a dataset."
)
class TypesCacheFactory extends TaskActivityFactory[DatasetSpec, TypesCache] {

  override def autoRun = true

  def apply(task: ProjectTask[DatasetSpec]): Activity[Types] = {
    new CachedActivity(
      activity = new TypesCache(task),
      resource = task.project.cacheResources.child("dataset").get(s"${task.id}_cache.xml")
    )
  }
}
