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
import org.silkframework.config.{PlainTask, RuntimeConfig, Task}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec}
import org.silkframework.entity.{Entity, EntitySchema, Index, Path}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.Uri

@Plugin(id = "cache", label = "Cache", description= "Reads the entities from an existing Silk entity cache.")
case class CacheDataset(dir: String) extends Dataset {

  private val file = new File(dir)

  override def source(implicit userContext: UserContext) = CacheSource

  override def entitySink(implicit userContext: UserContext) = ???

  override def linkSink(implicit userContext: UserContext) = ???

  object CacheSource extends DataSource {
    override def retrieve(entityDesc: EntitySchema, limit: Option[Int])
                         (implicit userContext: UserContext): Traversable[Entity] = {
      val entityCache = new FileEntityCache(entityDesc, _ => Index.default, file, RuntimeConfig(reloadCache = false))

      entityCache.readAll
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext): Seq[Entity] = Seq.empty

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext): Traversable[(String, Double)] = Traversable.empty

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext): IndexedSeq[Path] = IndexedSeq.empty

    override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask("cache_source", DatasetSpec(CacheDataset.this))
  }
}