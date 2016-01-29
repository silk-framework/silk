package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "transformPathsCache",
  label = "Transform Paths Cache",
  categories = Array("TransformSpecification"),
  description = "Holds the most frequent paths for the selected entities."
)
case class TransformPathsCacheFactory() extends TaskActivityFactory[TransformSpecification, TransformPathsCache] {

  override def autoRun = true

  def apply(task: Task[TransformSpecification]) = {
    new CachedActivity(
      activity = new TransformPathsCache(task),
      resource = task.project.cacheResources.child("transform").child(task.name).get(s"pathsCache.xml")
    )
  }
}
