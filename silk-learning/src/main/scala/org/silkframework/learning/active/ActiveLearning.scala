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
import org.silkframework.learning.active.linkselector.WeightedLinkageRule
import org.silkframework.learning.cleaning.CleanPopulationTask
import org.silkframework.learning.generation.{GeneratePopulation, LinkageRuleGenerator}
import org.silkframework.learning.reproduction.{Randomize, Reproduction}
import org.silkframework.learning.{LearningConfiguration, LearningException}
import org.silkframework.rule.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{DPair, Timer}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.linking.ReferenceEntitiesCache

import scala.math.max
import scala.util.Random

class ActiveLearning(task: ProjectTask[LinkSpec],
                     config: LearningConfiguration,
                     initialState: ActiveLearningState) extends Activity[ActiveLearningState] {

  override def initialValue: Option[ActiveLearningState] = Some(initialState)

  override def run(context: ActivityContext[ActiveLearningState])
                  (implicit userContext: UserContext): Unit = {

    val linkSpec = task.data
    val datasets = task.dataSources
    val referenceEntities = getReferenceEntities(context)
    implicit val prefixes: Prefixes = task.project.config.prefixes
    implicit val pluginContext: PluginContext = PluginContext.fromProject(task.project)

    // Update random seed
    val newRandomSeed = new Random(context.value().randomSeed).nextLong()
    context.value() = context.value().copy(randomSeed = newRandomSeed)
    implicit val random: Random = new Random(newRandomSeed)

    // Update unlabeled pool
    val pool = updatePool(linkSpec, datasets, context)

    // Create linkage rule generator
    val generator = linkageRuleGenerator(context)

    // Build initial population, if still empty
    buildPopulation(linkSpec, generator, context)

    // Ensure that we got positive and negative reference links
    val completeEntities = CompleteReferenceLinks(referenceEntities, pool.links, context.value().population)
    val fitnessFunction = config.fitnessFunction(completeEntities)

    // Learn new population
    updatePopulation(generator, completeEntities, fitnessFunction, context)

    // Select link candidates from the pool
    selectLinks(generator, completeEntities, fitnessFunction, context)

    // Clean population
    cleanPopulation(referenceEntities, generator, fitnessFunction, context)
  }

  // Retrieves reference entities
  private def getReferenceEntities(context: ActivityContext[ActiveLearningState])(implicit userContext: UserContext) = {
    context.status.updateMessage("Waiting for reference entities cache")
    // Update reference entities cache
    val entitiesCache = task.activity[ReferenceEntitiesCache].control
    if(entitiesCache.status().isRunning) {
      entitiesCache.waitUntilFinished()
    }
    try {
      entitiesCache.startBlocking()
    } catch {
      case _: IllegalStateException =>
        // Someone started the cache in the meantime
        entitiesCache.waitUntilFinished()
    }

    // Check if all links have been loaded
    val referenceEntities = entitiesCache.value()
    val entitiesSize = referenceEntities.positiveLinks.size + referenceEntities.negativeLinks.size
    val refSize = task.data.referenceLinks.positive.size + task.data.referenceLinks.negative.size
    assert(entitiesSize == refSize, "Reference Entities Cache has not been loaded correctly")

    referenceEntities
  }

  private def updatePool(linkSpec: LinkSpec,
                         datasets: DPair[DataSource],
                         context: ActivityContext[ActiveLearningState])
                        (implicit userContext: UserContext, prefixes: Prefixes, random: Random): UnlabeledLinkPool = Timer("Generating Pool") {
    var pool = context.value().pool

    // Build unlabeled pool
    if(context.value().pool.isEmpty) {
      context.status.updateMessage("Loading pool")
      val comparisonPairs = task.activity[ComparisonPairGenerator].value().selectedPairs
      if(comparisonPairs.isEmpty) {
        throw new LearningException("Cannot start active learning, because no comparison pairs have been selected.")
      }
      val generator = config.active.linkPoolGenerator.generator(datasets, linkSpec, comparisonPairs, random.nextLong())
      pool = context.child(generator, 0.5).startBlockingAndGetValue()

      if (pool.links.isEmpty) {
        throw new LearningException("Could not find any link candidates. Learning is not possible on this dataset(s).")
      }

      // Find matching paths
      context.value.updateWith(_.copy(comparisonPaths = comparisonPairs))
    }

    // Assert that no reference links are in the pool
    pool = pool.withoutLinks(linkSpec.referenceLinks.positive ++ linkSpec.referenceLinks.negative)
    if(pool.links.isEmpty) {
      throw new LearningException("All available link candidates have been confirmed or declined.")
    }

    // Update pool
    context.value() = context.value().copy(pool = pool)
    pool
  }

  private def linkageRuleGenerator(context: ActivityContext[ActiveLearningState])
                                  (implicit pluginContext: PluginContext): LinkageRuleGenerator = {


    if(context.value().generator.isEmpty) {
      val generator = LinkageRuleGenerator(context.value().comparisonPaths, config.components)
      context.value() = context.value().copy(generator = generator)
    }
    context.value().generator
  }

  private def buildPopulation(linkSpec: LinkSpec,
                              generator: LinkageRuleGenerator,
                              context: ActivityContext[ActiveLearningState])
                             (implicit userContext: UserContext, random: Random): Unit = Timer("Generating population") {
    var population = context.value().population
    if(population.isEmpty) {
      context.status.update("Generating population", 0.5)
      val seedRules = if(config.params.seed) linkSpec.rule :: Nil else Nil
      population = context.child(new GeneratePopulation(seedRules, generator, config, random.nextLong()), 0.3).startBlockingAndGetValue()
    }
    context.value() = context.value().copy(population = population)
  }

  private def updatePopulation(generator: LinkageRuleGenerator,
                               completeEntities: ReferenceEntities,
                               fitnessFunction: (LinkageRule => Double),
                               context: ActivityContext[ActiveLearningState])
                              (implicit userContext: UserContext, random: Random): Unit = Timer("Updating population") {
    context.status.update("Reproducing", 0.6)
    val targetFitness = if(context.value().population.isEmpty) 1.0 else context.value().population.bestIndividual.fitness
    var population = context.value().population
    for(i <- 0 until config.params.maxIterations
        if !context.status().isInstanceOf[Canceling]
        if i > 0 || population.bestIndividual.fitness < targetFitness
        if LinkageRuleEvaluator(population.bestIndividual.node.build, completeEntities).fMeasure < config.params.destinationfMeasure) {
      population = context.child(new Reproduction(population, fitnessFunction, generator, config, random.nextLong())).startBlockingAndGetValue()
      if(i % config.params.cleanFrequency == 0) {
        population = context.child(new CleanPopulationTask(population, fitnessFunction, generator, random.nextLong())).startBlockingAndGetValue()
      }
      context.status.updateProgress(0.6 + 0.2 / config.params.maxIterations, logStatus = false)
    }
    context.value() = context.value().copy(population = population)
  }

  private def selectLinks(generator: LinkageRuleGenerator,
                          completeEntities: ReferenceEntities,
                          fitnessFunction: (LinkageRule => Double),
                          context: ActivityContext[ActiveLearningState])
                         (implicit userContext: UserContext, random: Random): Unit = Timer("Selecting links") {
    if (!context.status().isInstanceOf[Canceling]) {
      context.status.update("Selecting evaluation links", 0.8)

      //TODO measure improvement of randomization
      val randomizedPopulation = context.child(new Randomize(context.value().population, fitnessFunction, generator, config, random.nextLong())).startBlockingAndGetValue()

      val weightedRules = {
        val bestFitness = randomizedPopulation.bestIndividual.fitness
        val topIndividuals = randomizedPopulation.individuals.filter(_.fitness >= bestFitness * 0.1).sortBy(-_.fitness)
        for (individual <- topIndividuals) yield {
          new WeightedLinkageRule(individual.node.build.operator, max(0.0001, individual.fitness))
        }
      }

      val updatedLinks = config.active.selector(weightedRules, context.value().pool.links, completeEntities)
      context.value() = context.value().copy(links = updatedLinks)
      for(topLink <- updatedLinks.headOption)
        context.log.fine(s"Selected top link candidate $topLink using ${config.active.selector.toString}")
    }
  }

  private def cleanPopulation(referenceEntities: ReferenceEntities,
                              generator: LinkageRuleGenerator,
                              fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState])
                             (implicit userContext: UserContext, random: Random): Unit = {
    if(referenceEntities.isDefined && !context.status().isInstanceOf[Canceling]) {
      val cleanedPopulation = context.child(new CleanPopulationTask(context.value().population, fitnessFunction, generator, random.nextLong())).startBlockingAndGetValue()
      context.value() = context.value().copy(population = cleanedPopulation)
    }
  }
}