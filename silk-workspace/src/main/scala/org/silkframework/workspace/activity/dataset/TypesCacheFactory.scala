package org.silkframework.workspace.activity.dataset

import org.silkframework.dataset.Dataset
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.DatasetActivityFactory

@Plugin(
  id = "TypesCache",
  label = "Types cache",
  categories = Array("Dataset"),
  description = "Holds the most frequent types in a dataset."
)
class TypesCacheFactory extends DatasetActivityFactory[Dataset, TypesCache] {

  override def autoRun: Boolean = true

  def apply(task: ProjectTask[GenericDatasetSpec]): Activity[Types] = {
    new TypesCache(task)
  }
}
