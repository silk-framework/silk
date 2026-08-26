package org.silkframework.workspace

import org.silkframework.config.{HasMetaData, MetaData, Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.templating.{CombinedTemplateVariablesReader, GlobalTemplateVariables, TemplateVariablesManager, TemplateVariablesReader}
import org.silkframework.util.Identifier

import scala.reflect.ClassTag

/**
  * Defines the API of a Project.
  */
trait ProjectTrait extends HasMetaData {
  /**
    * The name of this project.
    */
  def id: Identifier

  /** The project configuration. */
  def config: ProjectConfig

  /**
    * Retrieves the metadata for this project.
    */
  def metaData: MetaData = config.metaData

  /**
    * Access to the template variables for this project.
    */
  val templateVariables: TemplateVariablesManager

  /**
    * Combined access to the global and project template variables.
    */
  def combinedTemplateVariables: TemplateVariablesReader = {
    CombinedTemplateVariablesReader(Seq(GlobalTemplateVariables, templateVariables))
  }

  /** All tasks of a specific type. */
  def tasks[T <: TaskSpec : ClassTag](implicit userContext: UserContext): Seq[Task[T]]

  /** All tasks of all types. */
  def allTasks(implicit userContext: UserContext): Seq[Task[_ <: TaskSpec]]

  /** Returns a task option */
  def taskOption[T <: TaskSpec : ClassTag](taskName: Identifier)
                                          (implicit userContext: UserContext): Option[Task[T]]

  /** Returns a task option for an optional task name, e.g., an optional task input. */
  def taskOption[T <: TaskSpec : ClassTag](taskName: Option[Identifier])
                                          (implicit userContext: UserContext): Option[Task[T]] = {
    taskName.flatMap(name => taskOption[T](name))
  }

  /** Retrieves a task of the requested type. */
  def task[T <: TaskSpec : ClassTag](taskName: Identifier)
                                    (implicit userContext: UserContext): Task[T]

  /** The resource manager for that project. */
  def resources: ResourceManager

  /** Any task with that identifier. */
  def anyTaskOption(taskName: Identifier)
                   (implicit userContext: UserContext): Option[Task[_ <: TaskSpec]]

  /** Retrieves a task of any type. */
  def anyTask(taskName: Identifier)
             (implicit userContext: UserContext): Task[_ <: TaskSpec]
}
