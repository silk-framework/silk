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

package de.fuberlin.wiwiss.silk.workspace.modules.linking

import scala.xml.Node
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.runtime.task._
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

/**
 * Holds all caches.
 */
class LinkingCaches() extends HasStatus {

  /** The paths cache. */
  val pathCache = new PathsCache()

  /** The reference entities cache. */
  val referenceEntitiesCache = new ReferenceEntitiesCache(pathCache)

  /** Unlabeled Pool Cache. */
  //val poolCache =

  /** All caches. */
  val caches: Seq[Cache[LinkingTask, _]] = pathCache :: referenceEntitiesCache :: Nil

  //Update overall status whenever the status of a cache changes.
  pathCache.onUpdate(StatusListener)
  referenceEntitiesCache.onUpdate(StatusListener)

  /** The cached entity descriptions containing the most frequent paths. */
  def entityDescs = pathCache.value

  /** The cached entities. */
  def entities = referenceEntitiesCache.value

  /**
   * Reloads the cache.
   */
  def reload(project : Project, task: LinkingTask) {
    pathCache.clear()
    pathCache.load(project, task)

    referenceEntitiesCache.clear()
    referenceEntitiesCache.load(project, task)
  }

  /**
   * Loads the cache.
   */
  def load(project : Project, task: LinkingTask) {
    pathCache.load(project, task)
    referenceEntitiesCache.load(project, task)
  }

  /**
   * Blocks until all caches have been loaded
   */
  def waitUntilLoaded() {
    pathCache.waitUntilLoaded()
    referenceEntitiesCache.waitUntilLoaded()
  }

  /**
   * Serializes the caches to XML.
   */
  def toXML(implicit prefixes: Prefixes): Node = {
    <Caches>
      <Paths>
        { pathCache.toXML }
      </Paths>
      <Entities>
        { referenceEntitiesCache.toXML }
      </Entities>
    </Caches>
  }

  /**
   * Loads the values of the caches from XML.
   */
  def loadFromXML(node: Node) {
    pathCache.loadFromXML(node \ "Paths" \ "_" head)
    referenceEntitiesCache.loadFromXML(node \ "Entities" \ "_" head)
  }

  object StatusListener extends (TaskStatus => Unit) {
    def apply(status: TaskStatus) {
      //Collect the statuses of all caches
      val statuses = caches.map(_.status)

      //Check if all tasks are idle
      if(statuses.forall(_.isInstanceOf[TaskIdle])) {
        updateStatus(TaskIdle())
      }
      //Check if a task has failed
      else if (statuses.exists(_.failed)) {
        for(failedStatus <- statuses.find(_.failed)) updateStatus(failedStatus)
      }
      //Check if all tasks are finished
      else if(statuses.forall(_.isInstanceOf[TaskFinished])) {
        updateStatus(status)
      }
      //If we get here, at least one task is still running
      else {
        val overallProgress = statuses.map(_.progress).sum / caches.size
        updateStatus(status.message, overallProgress)
      }
    }
  }
}

