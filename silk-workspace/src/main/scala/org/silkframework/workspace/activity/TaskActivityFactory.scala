package org.silkframework.workspace.activity

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.{Activity, HasValue}
import org.silkframework.workspace.ProjectTask

import scala.reflect.ClassTag

/**
  * Factory for generating activities that belong to a task.
  *
  * @tparam TaskType The type of the task the generate activities belong to
  * @tparam ActivityType The type of activity that is generated and by which the activity will be identified within the task
  */
abstract class TaskActivityFactory[TaskType <: TaskSpec : ClassTag, ActivityType <: HasValue : ClassTag] extends WorkspaceActivityFactory
    with (ProjectTask[TaskType] => Activity[ActivityType#ValueType]) {

  /** True, if this activity shall be executed automatically after startup */
  def autoRun: Boolean = false

  /**
    * Should only generate activities for tasks for which this method returns true.
    */
  def generateForTask(task: TaskType): Boolean = true

  /**
    * Generates a new activity for a given task.
    */
  def apply(task: ProjectTask[TaskType]): Activity[ActivityType#ValueType]

  /**
    * Returns the type of the task for which this factory generates activities.
    */
  def taskType: Class[_] = implicitly[ClassTag[TaskType]].runtimeClass

  /**
    * Returns the type of generated activities.
    */
  def activityType: Class[_] = implicitly[ClassTag[ActivityType]].runtimeClass
}