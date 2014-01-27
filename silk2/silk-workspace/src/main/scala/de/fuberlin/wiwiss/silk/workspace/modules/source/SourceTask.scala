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

import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingCaches

/**
 * A data source.
 */
class SourceTask private(val source : Source, val cache: TypesCache) extends ModuleTask {

  val name = source.id
}

object SourceTask {
  /**
   * Constructs a new source task and starts loading the cache.
   */
  def apply(project: Project, source: Source, cache: TypesCache = new TypesCache()) = {
    val task = new SourceTask(source, cache)
    task.cache.load(project, task)
    task
  }
}

