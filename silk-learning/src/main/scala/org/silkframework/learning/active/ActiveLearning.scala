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

import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.active.linkselector.WeightedLinkageRule
import org.silkframework.learning.cleaning.CleanPopulationTask
import org.silkframework.learning.generation.{GeneratePopulation, LinkageRuleGenerator}
import org.silkframework.learning.reproduction.{Randomize, Reproduction}
import org.silkframework.rule.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.runtime.activity.Status.Canceling
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.{DPair, Timer}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.linking.LinkingTaskUtils._
import org.silkframework.workspace.activity.linking.{LinkingPathsCache, ReferenceEntitiesCache}

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
    val paths = getPaths()
    val referenceEntities = getReferenceEntities()

    // Update random seed
    val newRandomSeed = new Random(context.value().randomSeed).nextLong()
    context.value() = context.value().copy(randomSeed = newRandomSeed)
    implicit val random: Random = new Random(newRandomSeed)

    // Update unlabeled pool
    val pool = updatePool(linkSpec, datasets, context, paths)

    // Create linkage rule generator
    val generator = linkageRuleGenerator(context, referenceEntities)

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

  // Retrieves available paths
  private def getPaths() = {
    val pathsCache = task.activity[LinkingPathsCache].control
    pathsCache.waitUntilFinished()

    // Check if we got any paths
    val paths = pathsCache.value().map(_.typedPaths)
    assert(paths.source.nonEmpty, "No paths have been found in the source dataset (in LinkingPathsCache).")
    assert(paths.target.nonEmpty, "No paths have been found in the target dataset (in LinkingPathsCache).")

    paths
  }

  // Retrieves reference entities
  private def getReferenceEntities()(implicit userContext: UserContext) = {
    // Update reference entities cache
    val entitiesCache = task.activity[ReferenceEntitiesCache].control
    entitiesCache.waitUntilFinished()
    entitiesCache.startBlocking()

    // Check if all links have been loaded
    val referenceEntities = entitiesCache.value()
    val entitiesSize = referenceEntities.positiveLinks.size + referenceEntities.negativeLinks.size
    val refSize = task.data.referenceLinks.positive.size + task.data.referenceLinks.negative.size
    assert(entitiesSize == refSize, "Reference Entities Cache has not been loaded correctly")

    referenceEntities
  }

  private def updatePool(linkSpec: LinkSpec,
                         datasets: DPair[DataSource],
                         context: ActivityContext[ActiveLearningState],
                         paths: DPair[IndexedSeq[TypedPath]])
                        (implicit userContext: UserContext, random: Random): UnlabeledLinkPool = Timer("Generating Pool") {
    var pool = context.value().pool

    //Build unlabeled pool
    val poolPaths = context.value().pool.entityDescs.map(_.typedPaths)
    if(context.value().pool.isEmpty || poolPaths != paths) {
      context.status.updateMessage("Loading pool")
      val pathPairs =
        if(paths.source.toSet.diff(paths.target.toSet).size <= paths.source.size.toDouble * 0.1) {
          // If boths sources share most path, assume that the schemata are equal and generate direct pairs
          for((source, target) <- paths.source zip paths.target) yield DPair(source, target)
        } else {
          // If both source have different paths, generate the complete cartesian product
          for (sourcePath <- paths.source; targetPath <- paths.target) yield DPair(sourcePath, targetPath)
        }
      val generator = config.active.linkPoolGenerator.generator(datasets, linkSpec, pathPairs, random.nextLong())
      pool = context.child(generator, 0.5).startBlockingAndGetValue()
    }

    //Assert that no reference links are in the pool
    pool = pool.withoutLinks(linkSpec.referenceLinks.positive ++ linkSpec.referenceLinks.negative)

    // Update pool
    context.value() = context.value().copy(pool = pool)
    pool
  }

  private def linkageRuleGenerator(context: ActivityContext[ActiveLearningState],
                                   referenceEntities: ReferenceEntities): LinkageRuleGenerator = {
    val generator = Timer("LinkageRuleGenerator") {
      LinkageRuleGenerator(referenceEntities merge ReferenceEntities.fromEntities(context.value().pool.links.map(_.entities.get), Nil), config.components)
    }
    context.value() = context.value().copy(generator = generator)
    generator
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

      val updatedLinks = config.active.selector(weightedRules, context.value().pool.links.toSeq, completeEntities)
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