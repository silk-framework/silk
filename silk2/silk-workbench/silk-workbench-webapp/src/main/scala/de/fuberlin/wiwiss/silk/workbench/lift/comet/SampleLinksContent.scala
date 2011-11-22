/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, TaskDataListener}
import de.fuberlin.wiwiss.silk.workbench.learning._

class SampleLinksContent extends LinksContent with RateLinkButtons {

  override protected val showDetails = false

  override protected val showEntities = true

  override protected val showStatus = false

  private val linkListener = new TaskDataListener(CurrentValidationLinks) {
    override def onUpdate(links: Seq[Link]) {
      partialUpdate(updateLinksCmd)
    }
  }

  override protected def links: Seq[EvalLink] = {
    def links = linkingTask.referenceLinks

    for (link <- CurrentValidationLinks().view) yield {
      if (links.positive.contains(link))
        new EvalLink(link, Correct, Generated)
      else if (links.negative.contains(link))
        new EvalLink(link, Incorrect, Generated)
      else
        new EvalLink(link, Unknown, Generated)
    }
  }.sortBy(_.confidence.get.abs)

  override protected def updateReferenceLinks(referenceLinks: ReferenceLinks) {
    super.updateReferenceLinks(referenceLinks)
    startActiveLearningTask()
  }

  private def startActiveLearningTask() {
    User().linkingTask.cache.waitUntilLoaded()

    val task =
      new ActiveLearningTask(
        config = CurrentConfiguration(),
        sources = User().project.sourceModule.tasks.map(_.source),
        linkSpec = User().linkingTask.linkSpec,
        paths = User().linkingTask.cache.entityDescs.map(_.paths),
        referenceEntities = User().linkingTask.cache.entities,
        pool = CurrentPool(),
        population = CurrentPopulation()
      )

    CurrentValidationLinks() = Seq.empty
    CurrentActiveLearningTask() = task
    task.runInBackground()
  }
}