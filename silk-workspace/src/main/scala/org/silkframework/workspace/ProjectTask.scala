/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.workspace

import org.silkframework.config._
import org.silkframework.runtime.activity.{HasValue, Status, UserContext, ValueHolder}
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.activity.{CachedActivity, TaskActivity, TaskActivityFactory}

import java.time.Instant
import java.util.logging.{Level, Logger}
import scala.reflect.ClassTag
import scala.util.control.NonFatal


/**
  * A task that belongs to a project that has been loaded into the workspace.
  * [[ProjectTask]] instances are mutable, this means that the task data returned on successive calls can be different.
  *
  * @tparam TaskType The data type that specifies the properties of this task.
  */
class ProjectTask[TaskType <: TaskSpec : ClassTag](val id: Identifier,
                                                   private val initialData: TaskType,
                                                   private val initialMetaData: MetaData,
                                                   private val module: Module[TaskType]) extends Task[TaskType] {

  private val log = Logger.getLogger(getClass.getName)

  // Should be used to observe the task data
  val dataValueHolder: ValueHolder[TaskType] = new ValueHolder(Some(initialData))

  // Should be used to observe the meta data
  val metaDataValueHolder: ValueHolder[MetaData] = new ValueHolder(Some(
    // Make sure that the modified timestamp is set
    initialMetaData.copy(modified = Some(initialMetaData.modified.getOrElse(Instant.now)))
  ))

  lazy private val taskActivities: Seq[TaskActivity[TaskType, _ <: HasValue]] = {
    // Get all task activity factories for this task type
    implicit val pluginContext: PluginContext = PluginContext(module.project.config.prefixes, module.project.resources)
    val taskType = data.getClass
    val factories = PluginRegistry.availablePlugins[TaskActivityFactory[TaskType, _ <: HasValue]]
                                  .map(_.apply())
                                  .filter(a => a.taskType.isAssignableFrom(taskType) && a.generateForTask(data))
    var activities = List[TaskActivity[TaskType, _ <: HasValue]]()
    for (factory <- factories) {
      try {
        activities ::= new TaskActivity(this, factory)
      } catch {
        case NonFatal(ex) =>
          log.log(Level.WARNING, s"Could not load task activity '${factory.pluginSpec.id}' in task '$id' in project '${module.project.id}'.", ex)
      }
    }
    activities.reverse
  }

  lazy private val taskActivityMap: Map[Class[_], TaskActivity[TaskType, _ <: HasValue]] = taskActivities.map(a => (a.activityType, a)).toMap

  /**
    * The project this task belongs to.
    */
  def project: Project = module.project

  /**
    * Retrieves the current data of this task.
    */
  override def data: TaskType = dataValueHolder()

  /**
    * Retrieves the current meta data of this task.
    */
  override def metaData: MetaData = metaDataValueHolder()

  override def taskType: Class[_] = implicitly[ClassTag[TaskType]].runtimeClass

  /**
    * Starts all autorun activities.
    */
  def startActivities()(implicit userContext: UserContext): Unit = {
    for (activity <- taskActivities if shouldAutoRun(activity) && activity.status() == Status.Idle())
      activity.control.start()
  }

  /**
    * Cancels all activities.
    */
  def cancelActivities()(implicit userContext: UserContext): Unit = {
    for (activity <- taskActivities) {
      activity.control.cancel()
    }
  }

  private def shouldAutoRun(activity: TaskActivity[_, _]): Boolean = {
    // is auto-run activity
    activity.autoRun &&
    // do not run cached activities if auto-run is disabled for cached activities
        (Workspace.autoRunCachedActivities ||
            !classOf[CachedActivity[_]].isAssignableFrom(activity.factory.activityType))
  }

  /**
    * Updates the data of this task.
    */
  def update(newData: TaskType, newMetaData: Option[MetaData] = None)
            (implicit userContext: UserContext): Unit = synchronized {
    // Validate
    module.validator.validate(project, PlainTask(id, newData, newMetaData.getOrElse(metaData)))
    // Adapt meta data before saving
    // Update created and modified timestamps if not already set in new meta data object
    val oldMetaData = metaDataValueHolder()
    val metaDataToPersist = newMetaData.getOrElse(oldMetaData).copy(
      created = newMetaData.flatMap(_.created).orElse(oldMetaData.created),
      createdByUser = newMetaData.flatMap(_.createdByUser).orElse(oldMetaData.createdByUser),
      modified = Some(newMetaData.flatMap(_.modified).getOrElse(Instant.now)),
      lastModifiedByUser = newMetaData.flatMap(_.lastModifiedByUser).orElse(userContext.user.map(_.uri))
    )
    // First persist task
    persistTask(PlainTask.fromTask(ProjectTask.this).copy(data = newData, metaData = metaDataToPersist))
    // Update (in-memory) data
    dataValueHolder.update(newData)
    metaDataValueHolder.update(metaDataToPersist)
    // Restart each activity, don't wait for completion.
    for (activity <- taskActivities if shouldAutoRun(activity)) {
      activity.control.restart()
    }

    log.info(s"Updated task '$id' of project ${project.id}." + userContext.logInfo)
  }

  /**
    * Updates the meta data of this task.
    */
  def updateMetaData(newMetaData: MetaData)
                    (implicit userContext: UserContext): Unit = {
    update(dataValueHolder(), Some(newMetaData))
  }

  /**
    * All activities that belong to this task.
    */
  def activities: Seq[TaskActivity[TaskType, _ <: HasValue]] = taskActivities

  /**
    * Retrieves an activity by type.
    *
    * @tparam T The type of the requested activity
    * @return The activity control for the requested activity
    */
  def activity[T <: HasValue : ClassTag]: TaskActivity[TaskType, T] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    val act = taskActivityMap.getOrElse(requestedClass,
      throw new NoSuchElementException(s"Task '$id' in project '${project.id}' does not contain an activity of type '${requestedClass.getName}'. " +
        s"Available activities:\n${taskActivityMap.keys.map(_.getName).mkString("\n ")}"))
    act.asInstanceOf[TaskActivity[TaskType, T]]
  }

  /**
    * Retrieves an activity by name.
    *
    * @param activityName The name of the requested activity
    * @return The activity control for the requested activity
    * @throws NoSuchElementException If no activity with the specified name has been found.
    */
  def activity(activityName: String): TaskActivity[TaskType, _ <: HasValue] = {
    taskActivities.find(_.name == activityName)
        .getOrElse(throw new NoSuchElementException(s"Task '$id' in project '${project.id}' does not contain an activity named '$activityName'. " +
            s"Available activities: ${taskActivityMap.values.map(_.name).mkString(", ")}"))
  }

  /**
    * Finds all project tasks that reference this task.
    *
    * @param recursive Whether to return tasks that indirectly refer to this task.
    * @param ignoreTasks Set of tasks to be ignored in the dependency search.
    */
  override def findDependentTasks(recursive: Boolean, ignoreTasks: Set[Identifier] = Set.empty)
                                 (implicit userContext: UserContext): Set[Identifier] = {
    // Find all tasks that reference this task
    val dependentTasks = project.allTasks.filter(t => !ignoreTasks.contains(t.id) && t.data.referencedTasks.contains(id))

    var allDependentTaskIds = dependentTasks.map(_.id)
    if(recursive) {
      allDependentTaskIds ++= dependentTasks.flatMap(_.findDependentTasks(recursive = true, ignoreTasks + id))
    }
    allDependentTaskIds.distinct.toSet
  }

  override def findRelatedTasksInsideWorkflows()(implicit userContext: UserContext): Set[Identifier] = {
    val relatedWorkflowItems = for(workflow <- project.tasks[Workflow];
        workflowNode <- workflow.data.nodes if workflowNode.inputs.contains(Some(id.toString)) || workflowNode.outputs.contains(id.toString)) yield {
      workflowNode.task
    }
    relatedWorkflowItems.toSet
  }

  private def persistTask(task: Task[TaskType])(implicit userContext: UserContext): Unit = {
    // Write task
    module.provider.putTask(project.id, task, module.project.resources)
  }

  override def toString: String = {
    s"ProjectTask(id=$id, data=${dataValueHolder().toString}, metaData=${metaData.toString})"
  }

  // Returns all non-empty meta data fields as key value pairs
  def metaDataFields(): Seq[(String, String)] = {
    // ID is part of the metaData
    var metaDataFields: Vector[(String, String)] = Vector(("Task identifier", id.toString))
    for(label <- metaData.label if label.trim != "") {
      metaDataFields = metaDataFields :+ "Label" -> label
    }
    metaData.description foreach { description =>
      metaDataFields = metaDataFields :+ "Description" -> description
    }
    metaDataFields
  }

  /**
    * Retrieves all tags for this task.
    */
  override def tags()(implicit userContext: UserContext): Set[Tag] = {
    metaData.tags.map(uri => project.tagManager.getTag(uri))
  }
}
