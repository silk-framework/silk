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

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}
import java.util.logging.Logger

import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue}
import org.silkframework.runtime.plugin.{PluginDescription, PluginRegistry}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.TaskActivityFactory

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag


/**
 * A task.
 *
 * @tparam DataType The data type that specifies the properties of this task.
 */
class Task[DataType: ClassTag](val name: Identifier, initialData: DataType,
                               module: Module[DataType]) {

  private val log = Logger.getLogger(getClass.getName)

  @volatile
  private var currentData: DataType = initialData

  @volatile
  private var scheduledWriter: Option[ScheduledFuture[_]] = None

  private val taskActivities: Seq[ActivityHolder] = {
    // Get all task activity factories for this task type
    val factories = PluginRegistry.availablePlugins[TaskActivityFactory[DataType, _, _]].map(_.apply()).filter(_.isTaskType[DataType])
    for(factory <- factories) yield
      new ActivityHolder(factory)
  }

  private val taskActivityMap: Map[Class[_], ActivityHolder] = taskActivities.map(a => (a.factory.activityType, a)).toMap

  /**
    * The project this task belongs to.
    */
  def project = module.project

  /**
   * Retrieves the current data of this task.
   */
  def data = currentData

  def init() = {
    // Start autorun activities
    for(activity <- taskActivities if activity.factory.autoRun)
      activity.control.start()
  }

  /**
   * Updates the data of this task.
   */
  def update(newData: DataType) = synchronized {
    // Update data
    currentData = newData
    // (Re)Schedule write
    for(writer <- scheduledWriter) {
      writer.cancel(false)
    }
    scheduledWriter = Some(Task.scheduledExecutor.schedule(Writer, Task.writeInterval, TimeUnit.SECONDS))
    log.info("Updated task '" + name + "'")
  }

  /**
   * All activities that belong to this task.
   */
  def activities: Seq[ActivityControl[_]] = taskActivities.map(_.control)

  def activityConfig(activityName: String): Map[String, String] = {
    val activity = taskActivities.find(_.name == activityName).get
    PluginDescription(activity.factory.getClass).parameterValues(activity.factory)
  }

  /**
   * Retrieves an activity by type.
   *
   * @tparam T The type of the requested activity
   * @return The activity control for the requested activity
   */
  def activity[T <: HasValue : ClassTag]: ActivityControl[T#ValueType] = {
    val requestedClass = implicitly[ClassTag[T]].runtimeClass
    val act = taskActivityMap.getOrElse(requestedClass, throw new NoSuchElementException(s"Task '$name' in project '${project.name}' does not contain an activity of type '${requestedClass.getName}'. " +
                                                                                         s"Available activities:\n${taskActivityMap.keys.map(_.getName).mkString("\n ")}"))
    act.control.asInstanceOf[ActivityControl[T#ValueType]]
  }

  /**
   * Retrieves an activity by name.
   *
   * @param activityName The name of the requested activity
   * @return The activity control for the requested activity
   */
  def activity(activityName: String): ActivityControl[_] = {
    taskActivities.find(_.name == activityName)
      .getOrElse(throw new NoSuchElementException(s"Task '$name' in project '${project.name}' does not contain an activity named '$activityName'. " +
                                                  s"Available activities: ${taskActivityMap.values.map(_.name).mkString(", ")}")).control
  }

  private object Writer extends Runnable {
    override def run(): Unit = {
      // Write task
      module.provider.putTask(project.name, name, data)
      log.info(s"Persisted task '$name' in project '${project.name}'")
      // Update caches
      for(activity <- taskActivities if activity.factory.autoRun) {
        activity.control.cancel()
        activity.control.start()
      }
    }
  }

  private class ActivityHolder(@volatile var factory: TaskActivityFactory[DataType, _, _]) {
    def name = control.name
    @volatile
    var control: ActivityControl[_] = Activity(factory(Task.this))
  }
}

object Task {

  /* Do not persist updates more frequently than this (in seconds) */
  private val writeInterval = 5

  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

}