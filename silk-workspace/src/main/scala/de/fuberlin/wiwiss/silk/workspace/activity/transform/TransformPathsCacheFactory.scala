package de.fuberlin.wiwiss.silk.workspace.activity.transform

import de.fuberlin.wiwiss.silk.config.TransformSpecification
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.workspace.Task
import de.fuberlin.wiwiss.silk.workspace.activity.{CachedActivity, TaskActivityFactory}

class TransformPathsCacheFactory extends TaskActivityFactory[TransformSpecification, TransformPathsCache, SparqlEntitySchema] {

  def apply(task: Task[TransformSpecification]) = {
    new CachedActivity(
      activity = new TransformPathsCache(task),
      resource = task.project.cacheResources.child("transform").child(task.name).get(s"pathsCache.xml")
    )
  }
}
