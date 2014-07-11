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

package de.fuberlin.wiwiss.silk.workspace.modules.output

import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleProvider
import de.fuberlin.wiwiss.silk.util.XMLUtils._

/**
 * The output module which encapsulates all data sinks.
 */
class OutputModuleProvider extends ModuleProvider[OutputConfig, OutputTask] {
  /**
   * Loads the configuration for this module.
   */
  override def loadConfig(resources: ResourceLoader) = OutputConfig()

  /**
   * Writes updated configuration for this module.
   */
  override def writeConfig(config: OutputConfig, resources: ResourceManager): Unit = { }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[OutputTask] = {
    for(name <- resources.list) yield {
      val output = Output.load(project.resources)(resources.get(name + ".xml").load)
      OutputTask(output)
    }
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: OutputTask, resources: ResourceManager): Unit = {
    resources.put(task.name + ".xml") { os => task.output.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId + ".xml")
  }
}