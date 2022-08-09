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

package org.silkframework.learning.active

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.learning.active.comparisons.ComparisonPairGenerator
import org.silkframework.learning.active.poolgenerator.LinkPoolGeneratorUtils
import org.silkframework.learning.{LearningConfiguration, LearningException}
import org.silkframework.rule.LinkSpec
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, ActivityMonitor, UserContext}
import org.silkframework.util.{DPair, Timer}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.linking.{ReferenceEntitiesCache, ReferenceLinksEntityLoader}

import scala.util.Random

class ActiveLearningInitializer(task: ProjectTask[LinkSpec], config: LearningConfiguration) extends Activity[ActiveLearningReferenceData] {

  override def initialValue: Option[ActiveLearningReferenceData] = Some(ActiveLearningReferenceData.empty)

  override def run(context: ActivityContext[ActiveLearningReferenceData])
                  (implicit userContext: UserContext): Unit = {

    val linkSpec = task.data
    val datasets = task.dataSources
    implicit val prefixes: Prefixes = task.project.config.prefixes

    // Update random seed
    val randomSeed = task.activity[ComparisonPairGenerator].value().randomSeed
    implicit val random: Random = new Random(randomSeed)

    // Generate entity schemata
    context.value.updateWith(_.copy(entitySchemata = LinkPoolGeneratorUtils.entitySchema(linkSpec, task.activity[ComparisonPairGenerator].value().selectedPairs)))

    // Update unlabeled pool
    updatePool(linkSpec, datasets, context)

    // Retrieve reference links and their entities
    loadReferenceEntities(context)
  }

  private def updatePool(linkSpec: LinkSpec,
                         datasets: DPair[DataSource],
                         context: ActivityContext[ActiveLearningReferenceData])
                        (implicit userContext: UserContext, prefixes: Prefixes, random: Random): Unit = Timer("Generating Pool") {
    // Build unlabeled pool
    context.status.updateMessage("Loading pool")
    val comparisonPairs = task.activity[ComparisonPairGenerator].value().selectedPairs
    if (comparisonPairs.isEmpty) {
      throw new LearningException("Cannot start active learning, because no comparison pairs have been selected.")
    }
    val generator = config.active.linkPoolGenerator.generator(datasets, linkSpec, comparisonPairs, random.nextLong())
    val pool = context.child(generator, 0.5).startBlockingAndGetValue()

    if (pool.linkCandidates.isEmpty) {
      throw new LearningException("Could not find any link candidates. Learning is not possible on this dataset(s).")
    }

    // Update pool
    context.value.updateWith(_.copy(linkCandidates = pool.linkCandidates))
  }

  // Retrieves reference entities
  private def loadReferenceEntities(context: ActivityContext[ActiveLearningReferenceData])
                                   (implicit userContext: UserContext): Unit = {
    context.status.updateMessage("Waiting for reference entities cache")
    // Wait for reference entities cache
    val entitiesCache = task.activity[ReferenceEntitiesCache].control
    if(entitiesCache.status().isRunning) {
      entitiesCache.waitUntilFinished()
    }

    val referenceEntitiesMonitor = new ActivityMonitor[ReferenceEntities]("reference-entities", initialValue = Some(task.activity[ReferenceEntitiesCache].value()))
    val entityLoader = new ReferenceLinksEntityLoader(task, referenceEntitiesMonitor, context.value().entitySchemata, cancelled)
    entityLoader.load()

    // Check if all links have been loaded
    val referenceEntities = referenceEntitiesMonitor.value()
    val entitiesSize = referenceEntities.positiveLinks.size + referenceEntities.negativeLinks.size
    val refSize = task.data.referenceLinks.positive.size + task.data.referenceLinks.negative.size
    assert(entitiesSize == refSize, "Reference Entities Cache has not been loaded correctly")

    context.value.updateWith(_.copy(positiveLinks = referenceEntities.positiveLinks.toSeq, negativeLinks = referenceEntities.negativeLinks.toSeq))
  }
}