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

package org.silkframework.plugins.dataset

import java.io.File
import org.silkframework.cache.FileEntityCache
import org.silkframework.config.RuntimeConfig
import org.silkframework.dataset.{DataSource, DatasetPlugin}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.entity.{EntitySchema, Entity, Index}
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.FileUtils._
import org.silkframework.util.Uri

@Plugin(id = "cache", label = "Cache", description= "Reads the entities from an existing Silk entity cache.")
case class CacheDataset(dir: String) extends DatasetPlugin {

  private val file = new File(dir)

  override def source = CacheSource

  override def entitySink = ???

  override def linkSink = ???

  override def clear: Unit = { }

  object CacheSource extends DataSource {
    def retrieve(entityDesc: EntitySchema, limit: Option[Int]): Traversable[Entity] = {
      val entityCache = new FileEntityCache(entityDesc, _ => Index.default, file, RuntimeConfig(reloadCache = false))

      entityCache.readAll
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = Seq.empty
  }
}