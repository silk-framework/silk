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

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.Project
import scala.reflect.ClassTag


/**
 * A task.
 *
 * @tparam DataType The data type that specifies the properties of this task.
 */
class Task[DataType](val name: Identifier, initialData: DataType, val caches: Seq[Cache[DataType, _]], project: Project) {

  //TODO add change name function that asserts that the new name is unique

  private val log = Logger.getLogger(getClass.getName)

  private var currentData: DataType = initialData

  /* Do not persist updates more frequently than this (in milliseconds) */
  private val writeInterval = 5000L

  /**
   * Remember the time of the last write.
   */
  @volatile
  private var lastUpdateTime = 0L

  def data = currentData

  def update(newData: DataType) = {
    currentData = newData
    for(cache <- caches)
      cache.load(project, currentData, update = true)
    //TODO Schedule write!
    log.info("Updated task '" + name + "'")
  }

  def cache[T: ClassTag]: T = {
    val runtimeClass = implicitly[ClassTag[T]].runtimeClass
    caches.find(_.getClass == runtimeClass)
          .getOrElse(throw new NoSuchElementException(s"Task $name in project ${project.name} does not contain a cache of type ${runtimeClass.getName}"))
          .asInstanceOf[T]
  }

}