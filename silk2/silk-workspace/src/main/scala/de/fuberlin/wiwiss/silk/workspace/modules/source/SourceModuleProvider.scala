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

package de.fuberlin.wiwiss.silk.workspace.modules.source

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import scala.xml.XML

/**
 * The source module which encapsulates all data sources.
 */
class SourceModuleProvider extends ModuleProvider[SourceTask] {

  private val logger = Logger.getLogger(classOf[SourceModuleProvider].getName)

  override def prefix = "source"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, project: Project): Seq[SourceTask] = {
    val sources = resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
    for(name <- sources) yield {
      // Load the source
      val source = Source.load(project.resources)(resources.get(name).load)

      // Load the cache
      val cache = new TypesCache()
      try {
        cache.loadFromXML(XML.load(resources.get(source.id + "_cache.xml").load))
      } catch {
        case ex : Exception =>
          logger.log(Level.WARNING, "Cache corrupted. Rebuilding Cache.", ex)
          new LinkingCaches()
      }

      SourceTask(project, source, cache)
    }
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: SourceTask, resources: ResourceManager): Unit = {
    resources.put(task.name + ".xml"){ os => task.source.toXML.write(os) }
    resources.put(task.name + "_cache.xml") { os => task.cache.toXML.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(taskId: Identifier, resources: ResourceManager): Unit = {
    resources.delete(taskId + ".xml")
    resources.delete(taskId + "_cache.xml")
  }
}