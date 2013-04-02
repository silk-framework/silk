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

package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.util.Identifier

/**
 * A project module.
 */
trait Module[ConfigType <: ModuleConfig, TaskType <: ModuleTask] {
  /**
   * The configuration of this module
   */
  def config: ConfigType

  /**
   * Updates the configuration of this module.
   */
  def config_=(c: ConfigType)

  /**
   * Retrieves the tasks in this module.
   */
  def tasks: Traversable[TaskType]

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier): TaskType = {
    tasks.find(_.name == name).getOrElse(throw new NoSuchElementException("Task '" + name + "' not found."))
  }

  /**
   *  Updates a specific task.
   */
  def update(task: TaskType)

  /**
   * Removes a task from this project.
   */
  def remove(taskId: Identifier)
}
