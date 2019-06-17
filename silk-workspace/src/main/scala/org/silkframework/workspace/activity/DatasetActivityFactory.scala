package org.silkframework.workspace.activity

import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.runtime.activity.HasValue
import scala.reflect.ClassTag

abstract class DatasetActivityFactory[DatasetType <: Dataset : ClassTag, ActivityType <: HasValue : ClassTag]
  extends TaskActivityFactory[DatasetSpec[DatasetType], ActivityType] {

  def datasetType: Class[_] = implicitly[ClassTag[DatasetType]].runtimeClass

  override def generateForTask(task: DatasetSpec[DatasetType]): Boolean = {
    datasetType.isAssignableFrom(task.plugin.getClass)
  }

}
