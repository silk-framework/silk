package de.fuberlin.wiwiss.silk.workspace.activity.dataset

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.workspace.Task
import de.fuberlin.wiwiss.silk.workspace.activity.{CachedActivity, TaskActivityFactory}

class TypesCacheFactory extends TaskActivityFactory[Dataset, TypesCache, Types] {

  override def autoRun = true

  def apply(task: Task[Dataset]): Activity[Types] = {
    new CachedActivity(
      activity = new TypesCache(task.data),
      resource = task.project.cacheResources.child("dataset").get(s"${task.name}_cache.xml")
    )
  }
}
