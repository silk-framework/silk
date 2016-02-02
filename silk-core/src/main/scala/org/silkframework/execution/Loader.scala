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

package org.silkframework.execution

import java.util.logging.Level
import org.silkframework.cache.EntityCache
import org.silkframework.dataset.DataSource
import org.silkframework.runtime.activity.{ActivityContext, Activity}
import org.silkframework.util.DPair

/**
 * Loads the entity cache
 *
 * @param sampleSizeOpt Load all entities if set to None, else only load a sample of max. the configured size.
 */
class Loader(sources: DPair[DataSource],
             caches: DPair[EntityCache],
             sampleSizeOpt: Option[Int] = None) extends Activity[Unit] {

  override def name = "Loading"

  @volatile var exception: Exception = null

  @volatile var sourceLoader: LoadingThread = null
  @volatile var targetLoader: LoadingThread = null

  @volatile var canceled = false

  override def run(context: ActivityContext[Unit]) {
    canceled = false
    sourceLoader = new LoadingThread(context, true)
    targetLoader = new LoadingThread(context, false)

    sourceLoader.start()
    targetLoader.start()

    while ((sourceLoader.isAlive || targetLoader.isAlive) && !canceled) {
      Thread.sleep(100)
    }

    if (canceled) {
      sourceLoader.interrupt()
      targetLoader.interrupt()

      if (exception != null) {
        throw exception
      }
    }
  }

  override def cancelExecution() {
    canceled = true
    if (sourceLoader != null) sourceLoader.interrupt()
    if (targetLoader != null) targetLoader.interrupt()
  }

  class LoadingThread(context: ActivityContext[Unit], selectSource: Boolean) extends Thread {
    private val source = sources.select(selectSource)
    private val entityCache = caches.select(selectSource)

    private def retrieveEntities = {
      sampleSizeOpt match {
        case Some(sampleSize) =>
          source.sampleEntities(entityCache.entityDesc, sampleSize, None) // TODO: Add filter
        case None =>
          source.retrieveSparqlEntities(entityCache.entityDesc)
      }
    }

    override def run() {

      try {
        context.status.update("Loading entities of dataset " + source.toString)

        entityCache.clear()
        entityCache.write(retrieveEntities)
        entityCache.close()

        context.status.update(s"Entities loaded [ dataset :: ${source.toString} ].")

      } catch {
        case _: InterruptedException =>
          canceled = true
        case ex: Exception => {
          context.log.log(Level.WARNING, "Error loading resources", ex)
          exception = ex
          canceled = true
        }
      }
    }
  }

}
