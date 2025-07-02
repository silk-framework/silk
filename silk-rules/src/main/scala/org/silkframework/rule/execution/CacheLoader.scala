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

package org.silkframework.rule.execution

import org.silkframework.cache.EntityCache
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.Entity
import org.silkframework.runtime.activity.{Activity, ActivityContext, Status, UserContext}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.EmptyResourceManager

import scala.util.Random
import scala.util.control.Breaks._

/**
 * Loads the entity cache
 *
 * @param sampleSizeOpt Load all entities if set to None, else only load a sample of max. the configured size.
 */
class CacheLoader(source: DataSource,
                  entityCache: EntityCache,
                  sampleSizeOpt: Option[Int] = None)
                 (implicit prefixes: Prefixes) extends Activity[Unit] {

  override def name = "Loading"

  override def run(context: ActivityContext[Unit])
                  (implicit userContext: UserContext): Unit = {
    context.status.updateMessage("Loading entities of dataset " + source.toString)
    entityCache.clear()
    load(context)
    entityCache.close()
    context.status.updateMessage(s"Entities loaded [ dataset :: ${source.toString} ].")
  }

  private def load(context: ActivityContext[Unit])(implicit userContext: UserContext) = {
    val startTime = System.currentTimeMillis()
    var entityCounter = 0
    retrieveEntities.use { entities =>
      while(entities.hasNext && !(context.status().isInstanceOf[Status.Canceling] || cancelled)) {
        entityCache.write(entities.next())
        entityCounter += 1
      }
    }

    val time = (System.currentTimeMillis - startTime) / 1000.0
    context.log.info("Finished writing " + entityCounter + " entities with type '" + entityCache.entitySchema.typeUri +
      "' in " + time + " seconds." + context.status.projectAndTaskIdString)
  }

  private def retrieveEntities(implicit userContext: UserContext): CloseableIterator[Entity] = {
    implicit val pluginContext: PluginContext = PluginContext(prefixes = prefixes, resources = EmptyResourceManager(), user = userContext)
    sampleSizeOpt match {
      case Some(sampleSize) =>
        implicit val random: Random = Random
        CloseableIterator(source.sampleEntities(entityCache.entitySchema, sampleSize, None)) // TODO: Add filter
      case None =>
        source.retrieve(entityCache.entitySchema).entities
    }
  }
}
