package org.silkframework.workspace.activity.dataset

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity.Activity
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

class TypesCacheFactory extends TaskActivityFactory[Dataset, TypesCache, Types] {

  override def autoRun = true

  def apply(task: Task[Dataset]): Activity[Types] = {
    new CachedActivity(
      activity = new TypesCache(task.data),
      resource = task.project.cacheResources.child("dataset").get(s"${task.name}_cache.xml")
    )
  }
}
