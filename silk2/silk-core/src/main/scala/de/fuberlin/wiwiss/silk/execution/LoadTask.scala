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

package de.fuberlin.wiwiss.silk.execution

import java.util.logging.Level
import de.fuberlin.wiwiss.silk.cache.EntityCache
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.runtime.task.{Future, Task, TaskFinished}
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * Loads the entity cache
 */
class LoadTask(sources: DPair[DataSource],
               caches: DPair[EntityCache]) extends Task[Unit] {

  taskName = "Loading"

  @volatile var exception: Exception = null

  @volatile var sourceLoader: LoadingThread = null
  @volatile var targetLoader: LoadingThread = null

  @volatile var canceled = false

  override def execute() {
    canceled = false
    sourceLoader = new LoadingThread(true)
    targetLoader = new LoadingThread(false)

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

  /**
   * Executes this task in the background.
   * Returns as soon as both caches are being written.
   */
  override def runInBackground(): Future[Unit] = {
    val future = super.runInBackground()

    //Wait until the caches are being written
    while (!status.isInstanceOf[TaskFinished] && !(caches.source.isWriting && caches.target.isWriting)) {
      Thread.sleep(100)
    }

    future
  }

  override def stopExecution() {
    canceled = true
    if(sourceLoader != null) sourceLoader.interrupt()
    if(targetLoader != null) targetLoader.interrupt()
  }

  class LoadingThread(selectSource: Boolean) extends Thread {
    private val source = sources.select(selectSource)
    private val entityCache = caches.select(selectSource)

    override def run() {

      try {
        updateStatus("Loading entities of dataset " + source.toString)

        entityCache.clear()
        entityCache.write(source.retrieve(entityCache.entityDesc))
        entityCache.close()

        updateStatus(s"Entities loaded [ dataset :: ${source.toString} ].")

      } catch {
        case ex: Exception => {
          logger.log(Level.WARNING, "Error loading resources", ex)
          exception = ex
          canceled = true
        }
      }
    }
  }

}
