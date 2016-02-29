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

import org.silkframework.config.LinkSpecification
import org.silkframework.dataset.DataSource
import org.silkframework.entity.Path
import org.silkframework.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.active.linkselector.WeightedLinkageRule
import org.silkframework.learning.cleaning.CleanPopulationTask
import org.silkframework.learning.generation.{GeneratePopulation, LinkageRuleGenerator}
import org.silkframework.learning.reproduction.{Randomize, Reproduction}
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.util.{DPair, Timer}

import scala.math.max

//TODO support canceling
class ActiveLearning(config: LearningConfiguration,
                     datasets: DPair[DataSource],
                     linkSpec: LinkSpecification,
                     paths: DPair[Seq[Path]],
                     referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                     initialState: ActiveLearningState = ActiveLearningState.initial) extends Activity[ActiveLearningState] {

  def isEmpty = datasets.isEmpty

  override def initialValue = Some(initialState)

  override def run(context: ActivityContext[ActiveLearningState]): Unit = {
    // Update unlabeled pool
    val pool = updatePool(context)

    // Create linkage rule generator
    val generator = linkageRuleGenerator(context)

    // Build initial population, if still empty
    buildPopulation(generator, context)

    // Ensure that we got positive and negative reference links
    val completeEntities = CompleteReferenceLinks(referenceEntities, pool.links, context.value().population)
    val fitnessFunction = config.fitnessFunction(completeEntities)

    // Learn new population
    updatePopulation(generator, completeEntities, fitnessFunction, context)

    // Select link candidates from the pool
    selectLinks(generator, completeEntities, fitnessFunction, context)

    // Clean population
    cleanPopulation(generator, fitnessFunction, context)
  }
  
  private def updatePool(context: ActivityContext[ActiveLearningState]): UnlabeledLinkPool = Timer("Generating Pool") {
    var pool = context.value().pool

    //Build unlabeled pool
    val poolPaths = context.value().pool.entityDescs.map(_.paths)
    if(context.value().pool.isEmpty || poolPaths != paths) {
      context.status.updateMessage("Loading pool")
      val generator = config.active.linkPoolGenerator.generator(datasets, linkSpec, paths)
      pool = context.child(generator, 0.5).startBlockingAndGetValue()
    }

    //Assert that no reference links are in the pool
    pool = pool.withoutLinks(linkSpec.referenceLinks.positive ++ linkSpec.referenceLinks.negative)

    // Update pool
    context.value() = context.value().copy(pool = pool)
    pool
  }

  private def linkageRuleGenerator(context: ActivityContext[ActiveLearningState]): LinkageRuleGenerator = {
    val generator = Timer("LinkageRuleGenerator") {
      LinkageRuleGenerator(referenceEntities merge ReferenceEntities.fromEntities(context.value().pool.links.map(_.entities.get), Nil), config.components)
    }
    context.value() = context.value().copy(generator = generator)
    generator
  }
  
  private def buildPopulation(generator: LinkageRuleGenerator, context: ActivityContext[ActiveLearningState]) = Timer("Generating population") {
    var population = context.value().population
    if(population.isEmpty) {
      context.status.update("Generating population", 0.5)
      val seedRules = if(config.params.seed) linkSpec.rule :: Nil else Nil
      population = context.child(new GeneratePopulation(seedRules, generator, config), 0.3).startBlockingAndGetValue()
    }
    context.value() = context.value().copy(population = population)
  }
  
  private def updatePopulation(generator: LinkageRuleGenerator, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState]) = Timer("Updating population") {
    context.status.update("Reproducing", 0.6)
    val targetFitness = if(context.value().population.isEmpty) 1.0 else context.value().population.bestIndividual.fitness
    var population = context.value().population
    for(i <- 0 until config.params.maxIterations
        if i > 0 || population.bestIndividual.fitness < targetFitness
        if LinkageRuleEvaluator(population.bestIndividual.node.build, completeEntities).fMeasure < config.params.destinationfMeasure) {
      population = context.child(new Reproduction(population, fitnessFunction, generator, config)).startBlockingAndGetValue()
      if(i % config.params.cleanFrequency == 0) {
        population = context.child(new CleanPopulationTask(population, fitnessFunction, generator)).startBlockingAndGetValue()
      }
      context.status.updateProgress(0.6 + 0.2 / config.params.maxIterations, logStatus = false)
    }
    context.value() = context.value().copy(population = population)
  }

  private def selectLinks(generator: LinkageRuleGenerator, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState]) = Timer("Selecting links") {
    context.status.update("Selecting evaluation links", 0.8)

    //TODO measure improvement of randomization
    val randomizedPopulation = context.child(new Randomize(context.value().population, fitnessFunction, generator, config)).startBlockingAndGetValue()

    val weightedRules = {
      val bestFitness = randomizedPopulation.bestIndividual.fitness
      val topIndividuals = randomizedPopulation.individuals.toSeq.filter(_.fitness >= bestFitness * 0.1).sortBy(-_.fitness)
      for(individual <- topIndividuals) yield {
        new WeightedLinkageRule(individual.node.build.operator, max(0.0001, individual.fitness))
      }
    }

    val updatedLinks = config.active.selector(weightedRules, context.value().pool.links.toSeq, completeEntities)
    context.value() = context.value().copy(links = updatedLinks)
    context.log.fine(s"Selected top link candidate ${updatedLinks.head} using ${config.active.selector.toString}")
  }

  private def cleanPopulation(generator: LinkageRuleGenerator, fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState]): Unit = {
    if(referenceEntities.isDefined) {
      val cleanedPopulation = context.child(new CleanPopulationTask(context.value().population, fitnessFunction, generator)).startBlockingAndGetValue()
      context.value() = context.value().copy(population = cleanedPopulation)
    }
  }
}