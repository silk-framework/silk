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

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.runtime.activity.Status
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.Cache

import scala.xml.Node

/**
 * Holds all caches.
 */
class LinkingCaches() extends Cache[LinkSpecification, Unit] {

  /** The paths cache. */
  val pathCache = new PathsCache()

  /** The reference entities cache. */
  val referenceEntitiesCache = new ReferenceEntitiesCache(pathCache)

  /** Unlabeled Pool Cache. */
  //val poolCache =

  /** All caches. */
  val caches: Seq[Cache[LinkSpecification, _]] = pathCache :: referenceEntitiesCache :: Nil

  //Update overall status whenever the status of a cache changes.
  pathCache.status.onUpdate(StatusListener)
  referenceEntitiesCache.status.onUpdate(StatusListener)

  /** The cached entity descriptions containing the most frequent paths. */
  def entityDescs: DPair[EntityDescription] = pathCache.entityDescs

  /** The cached entities. */
  def entities = referenceEntitiesCache.value

  /**
   * Reloads the cache.
   */
  def reload(project : Project, task: LinkSpecification) {
    pathCache.clear()
    pathCache.load(project, task)

    referenceEntitiesCache.clear()
    referenceEntitiesCache.load(project, task)
  }

  /**
   * Loads the cache.
   */
  override def load(project : Project, task: LinkSpecification, update: Boolean) {
    pathCache.load(project, task, update)
    referenceEntitiesCache.load(project, task, update)
  }

  /**
   * Blocks until all caches have been loaded
   */
  override def waitUntilLoaded() {
    pathCache.waitUntilLoaded()
    referenceEntitiesCache.waitUntilLoaded()
  }

  /**
   * Serializes the caches to XML.
   */
  override protected def serialize: Node = {
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
  override protected def deserialize(node: Node) {
    pathCache.loadFromXML(node \ "Paths" \ "_" head)
    referenceEntitiesCache.loadFromXML(node \ "Entities" \ "_" head)
  }

  // Never called as load method is overridden
  override protected def update(project: Project, task: LinkSpecification): Boolean = false

  object StatusListener extends (Status => Unit) {
    def apply(newStatus: Status) {
      //Collect the statuses of all caches
      val statuses = caches.map(_.status())

      //Check if all tasks are idle
      if(statuses.forall(_ == Status.Idle)) {
        status.update(Status.Idle)
      }
      //Check if a task has failed
      else if (statuses.exists(_.failed)) {
        for(failedStatus <- statuses.find(_.failed)) status.update(failedStatus)
      }
      //Check if all tasks are finished
      else if(statuses.forall(_.isInstanceOf[Status.Finished])) {
        status.update(newStatus)
      }
      //If we get here, at least one task is still running
      else {
        val overallProgress = statuses.map(_.progress).sum / caches.size
        status.update(newStatus.message, overallProgress)
      }
    }
  }
}

