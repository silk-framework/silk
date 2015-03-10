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

package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.dataset.{DataSource}
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulation, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.reproduction.{Randomize, Reproduction}
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import linkselector.WeightedLinkageRule
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.{Timer, DPair}
import math.max

//TODO support canceling
class ActiveLearning(config: LearningConfiguration,
                     datasets: DPair[DataSource],
                     linkSpec: LinkSpecification,
                     paths: DPair[Seq[Path]],
                     referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                     state: ActiveLearningState) extends Activity[ActiveLearningState] {

  private var pool = state.pool

  private var population = state.population

  private var links = state.links

  def isEmpty = datasets.isEmpty

  override def initialValue = state

  override def run(context: ActivityContext[ActiveLearningState]): Unit = {
    updatePool(context)

    val generator = Timer("LinkageRuleGenerator") {
      LinkageRuleGenerator(referenceEntities merge ReferenceEntities.fromEntities(state.pool.map(_.entities.get), Nil), config.components)
    }
    val targetFitness = if(state.population.isEmpty) 1.0 else state.population.bestIndividual.fitness
    
    buildPopulation(generator, context)

    val completeEntities = Timer("CompleteReferenceLinks") { CompleteReferenceLinks(referenceEntities, state.pool, state.population) }
    val fitnessFunction = config.fitnessFunction(completeEntities)
    
    updatePopulation(generator, targetFitness, completeEntities, fitnessFunction, context)

    //Select evaluation links
    context.status.update("Selecting evaluation links", 0.8)

    //TODO measure improvement of randomization
    selectLinks(generator, completeEntities, fitnessFunction, context)

    //Clean population
    if(referenceEntities.isDefined) {
      population = context.executeBlocking(new CleanPopulationTask(population, fitnessFunction, generator))
    }

    context.value.update(ActiveLearningState(pool, population, links))
  }
  
  private def updatePool(context: ActivityContext[ActiveLearningState]) = Timer("Generating Pool")  {
    //Build unlabeled pool
    if(context.value().pool.isEmpty) {
      context.status.update("Loading")
      context.executeBlocking(new GeneratePool(datasets, linkSpec, paths), 0.5)
    }

    //Assert that no reference links are in the pool
    pool = pool.filterNot(referenceEntities.positive.contains).filterNot(referenceEntities.negative.contains)
  }
  
  private def buildPopulation(generator: LinkageRuleGenerator, context: ActivityContext[ActiveLearningState]) = Timer("Generating population") {
    if(population.isEmpty) {
      context.status.update("Generating population", 0.5)
      val seedRules = if(config.params.seed) linkSpec.rule :: Nil else Nil
      population = context.executeBlocking(new GeneratePopulation(seedRules, generator, config), 0.6)
    }
  }
  
  private def updatePopulation(generator: LinkageRuleGenerator, targetFitness: Double, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState]) = Timer("Updating population") {
    for(i <- 0 until config.params.maxIterations
        if i > 0 || population.bestIndividual.fitness < targetFitness
        if LinkageRuleEvaluator(population.bestIndividual.node.build, completeEntities).fMeasure < config.params.destinationfMeasure) {
      val progress = 0.2 / config.params.maxIterations
      population = context.executeBlocking(new Reproduction(population, fitnessFunction, generator, config), progress)
      if(i % config.params.cleanFrequency == 0) {
        population = context.executeBlocking(new CleanPopulationTask(population, fitnessFunction, generator))
      }
    }
  }

  private def selectLinks(generator: LinkageRuleGenerator, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double), context: ActivityContext[ActiveLearningState]) = Timer("Selecting links") {
    val randomizedPopulation = context.executeBlocking(new Randomize(population, fitnessFunction, generator, config))

    val weightedRules = {
      val bestFitness = randomizedPopulation.bestIndividual.fitness
      val topIndividuals = randomizedPopulation.individuals.toSeq.filter(_.fitness >= bestFitness * 0.1).sortBy(-_.fitness)
      for(individual <- topIndividuals) yield {
        new WeightedLinkageRule(individual.node.build.operator, max(0.0001, individual.fitness))
      }
    }

    links = config.active.selector(weightedRules, pool.toSeq, completeEntities)
  }
}