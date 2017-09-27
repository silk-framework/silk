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

import java.io.OutputStreamWriter
import java.util.logging.Logger

import org.silkframework.config._
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Identifier

import scala.xml.XML

/**
 * Holds custom tasks.
 */
private class CustomTaskXmlSerializer extends XmlSerializer[CustomTask] {

  private val logger = Logger.getLogger(classOf[CustomTaskXmlSerializer].getName)

  override def prefix = "custom"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Seq[Task[CustomTask]] = {
    val names = resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
    val tasks = for (name <- names) yield {
      loadTask(name, resources, projectResources)
    }

    tasks
  }

  private def loadTask(name: String, resources: ResourceLoader, projectResources: ResourceManager) = {
    implicit val res = projectResources
    implicit val readContext = ReadContext(projectResources)
    XmlSerialization.fromXml[Task[CustomTask]](resources.get(name).read(XML.load))
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[CustomTask], resources: ResourceManager): Unit = {
    resources.get(task.id.toString + ".xml").write() { os =>
      val taskXml = XmlSerialization.toXml(task)
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
}