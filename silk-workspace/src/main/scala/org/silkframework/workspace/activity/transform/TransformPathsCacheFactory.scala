package org.silkframework.workspace.activity.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, TaskActivityFactory}

@Plugin(
  id = "TransformPathsCache",
  label = "Transform Paths Cache",
  categories = Array("TransformSpecification"),
  description = "Holds the most frequent paths for the selected entities."
)
case class TransformPathsCacheFactory() extends TaskActivityFactory[TransformSpec, TransformPathsCache] {

  override def autoRun: Boolean = true

  def apply(task: ProjectTask[TransformSpec]): CachedActivity[CachedEntitySchemata] = {
    new TransformPathsCache(task)
  }
}
