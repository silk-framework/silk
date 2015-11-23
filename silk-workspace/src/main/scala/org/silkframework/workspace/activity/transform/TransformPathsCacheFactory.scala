package org.silkframework.workspace.activity.transform

import org.silkframework.config.TransformSpecification
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

class TransformPathsCacheFactory extends TaskActivityFactory[TransformSpecification, TransformPathsCache, SparqlEntitySchema] {

  def apply(task: Task[TransformSpecification]) = {
    new CachedActivity(
      activity = new TransformPathsCache(task),
      resource = task.project.cacheResources.child("transform").child(task.name).get(s"pathsCache.xml")
    )
  }
}
