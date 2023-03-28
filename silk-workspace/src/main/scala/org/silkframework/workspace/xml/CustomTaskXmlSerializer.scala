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

package org.silkframework.workspace.xml

import org.silkframework.config._
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{WriteContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.LoadedTask

import java.io.OutputStreamWriter
import java.util.logging.Logger
import scala.xml.Node

/**
 * Holds custom tasks.
 */
private class CustomTaskXmlSerializer extends XmlSerializer[CustomTask] {

  private val logger = Logger.getLogger(classOf[CustomTaskXmlSerializer].getName)

  override def prefix = "custom"

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[CustomTask], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
    // Only serialize file paths correctly, paths should not be prefixed
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](resources = projectResourceManager)
    val taskXml = XmlSerialization.toXml(task)
    resources.get(task.id.toString + ".xml").write() { os =>
      val out = new OutputStreamWriter(os, "UTF-8")
      out.write(taskXml.toString())
      out.write("\n")
      out.flush()
    }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
    resources.delete(name.toString + "_cache.xml")
  }

  override def loadTasks(resources: ResourceLoader)
                        (implicit context: PluginContext): Seq[LoadedTask[CustomTask]] = {
    val names = taskNames(resources)
    val tasks = for (name <- names) yield {
      loadTaskSafelyFromXML(name, Some(name.stripSuffix(".xml")), resources)
    }

    tasks
  }

  private def taskNames(resources: ResourceLoader) = {
    resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
  }
}