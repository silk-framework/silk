package org.silkframework.workspace

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier

import scala.reflect.ClassTag

/**
  * Defines the API of a Project.
  */
trait ProjectTrait {
  /**
    * The name of this project.
    */
  def name: Identifier

  /** The project configuration. */
  def config: ProjectConfig

  /** All tasks of a specific type. */
  def tasks[T <: TaskSpec : ClassTag](implicit userContext: UserContext): Seq[Task[T]]

  /** Returns a task option */
  def taskOption[T <: TaskSpec : ClassTag](taskName: Identifier)
                                          (implicit userContext: UserContext): Option[Task[T]]

  /** The resource manager for that project. */
  def resources: ResourceManager

  /** Any task with that identifier. */
  def anyTaskOption(taskName: Identifier)
                   (implicit userContext: UserContext): Option[Task[_ <: TaskSpec]]
}
