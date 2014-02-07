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
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleTask
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.workspace.Project

/**
 * A linking task, which interlinks two data sets.
 */
class LinkingTask private(val linkSpec: LinkSpecification, val referenceLinks: ReferenceLinks, val cache: LinkingCaches) extends ModuleTask {
  val name = linkSpec.id

  def updateLinkSpec(linkSpec: LinkSpecification, project: Project) = {
    LinkingTask(project, linkSpec, referenceLinks, cache)
  }

  def updateReferenceLinks(referenceLinks: ReferenceLinks, project: Project) = {
    LinkingTask(project, linkSpec, referenceLinks, cache)
  }
}

object LinkingTask {
  /**
   * Constructs a new linking task and starts loading the cache.
   */
  def apply(project: Project, linkSpec: LinkSpecification, referenceLinks: ReferenceLinks = ReferenceLinks(), cache: LinkingCaches = new LinkingCaches()) = {
    val task = new LinkingTask(linkSpec, referenceLinks, cache)
    task.cache.load(project, task)
    task
  }
}