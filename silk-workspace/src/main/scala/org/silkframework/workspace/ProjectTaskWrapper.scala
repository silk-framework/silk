package org.silkframework.workspace

import org.silkframework.config.TaskSpec

import scala.reflect.ClassTag

/**
  * This is a single purpose wrapper for ProjectTasks. The companion object can be used to pass any given object along this task. (used currently to pass OutputStreams)
  * @param pt - the origin ProjectTask to be wrapped
  * @param companion -  the companion object
  * @tparam TaskType The data type that specifies the properties of this task.
  * @tparam CompanionType Type of the companion object
  */
class ProjectTaskWrapper[TaskType <: TaskSpec : ClassTag, CompanionType] (pt: ProjectTask[TaskType], val companion: CompanionType) extends ProjectTask[TaskType](pt) {
  //FIXME: this wrapper should not be necessary, reimplement Task so we can define any given output destination

}
