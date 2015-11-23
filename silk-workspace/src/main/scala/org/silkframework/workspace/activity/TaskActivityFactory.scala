package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.Activity
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.workspace.Task

import scala.reflect.ClassTag

/**
  * Factory for generating activities that belong to a task.
  *
  * @tparam TaskType The type of the task the generate activities belong to
  * @tparam ActivityType The type of activity that is generated and by which the activity will be identified within the task
  * @tparam ActivityData The type of the activity values.
  */
abstract class TaskActivityFactory[TaskType: ClassTag, ActivityType <: Activity[ActivityData] : ClassTag, ActivityData] extends AnyPlugin with (Task[TaskType] => Activity[ActivityData]) {

  /** True, if this activity shall be executed automatically after startup */
  def autoRun: Boolean = false

  /**
    * Generates a new activity for a given task.
    */
  def apply(task: Task[TaskType]): Activity[ActivityData]

  /**
    * Checks, if this factory generates activities for a given task type
    */
  def isTaskType[T: ClassTag]: Boolean = {
    implicitly[ClassTag[TaskType]].runtimeClass == implicitly[ClassTag[T]].runtimeClass
  }

  /**
    * Returns the type of generated activities.
    */
  def activityType = implicitly[ClassTag[ActivityType]].runtimeClass
}