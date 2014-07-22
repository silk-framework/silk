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

package de.fuberlin.wiwiss.silk.workspace.modules.dataset

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * A data source.
 */
class DatasetTask private(val dataset: Dataset, val cache: TypesCache) extends ModuleTask {

  val name = dataset.id

  def source = dataset.source

  def sink = dataset.sink
}

object DatasetTask {
  /**
   * Constructs a new source task and starts loading the cache.
   */
  def apply(project: Project, source: Dataset, cache: TypesCache = new TypesCache()) = {
    val task = new DatasetTask(source, cache)
    task.cache.load(project, task)
    task
  }
}

