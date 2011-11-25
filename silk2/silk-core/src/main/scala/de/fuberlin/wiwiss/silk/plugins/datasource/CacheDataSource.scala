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

package de.fuberlin.wiwiss.silk.plugins.datasource

import de.fuberlin.wiwiss.silk.datasource.DataSource
import java.io.File
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.cache.FileEntityCache
import de.fuberlin.wiwiss.silk.entity.{Index, Entity, EntityDescription}

@Plugin(id = "cache", label = "Cache", description= "Reads the entities from an existing Silk entity cache.")
case class CacheDataSource(dir: String) extends DataSource {
  private val file = new File(dir)

  def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    val entityCache = new FileEntityCache(entityDesc, _ => Index.default, file, RuntimeConfig(reloadCache = false))

    entityCache.readAll
  }
}