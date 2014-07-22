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

import de.fuberlin.wiwiss.silk.dataset.{Dataset}
import de.fuberlin.wiwiss.silk.runtime.task.ValueTask
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.learning.reproduction.{RandomizeTask, ReproductionTask}
import linkselector.WeightedLinkageRule
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.util.{Timer, DPair}
import math.max

//TODO support canceling
class ActiveLearningTask(config: LearningConfiguration,
                         dataset: Traversable[Dataset],
                         linkSpec: LinkSpecification,
                         paths: DPair[Seq[Path]],
                         referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                         var pool: Traversable[Link] = Traversable.empty,
                         var population: Population = Population.empty) extends ValueTask[Seq[Link]](Seq.empty) {

  def isEmpty = dataset.isEmpty

  def links = value.get

  override protected def execute(): Seq[Link] = {
    updatePool()

    val generator = Timer("LinkageRuleGenerator") {
      LinkageRuleGenerator(referenceEntities merge ReferenceEntities.fromEntities(pool.map(_.entities.get), Nil), config.components)
    }
    val targetFitness = if(population.isEmpty) 1.0 else population.bestIndividual.fitness
    
    buildPopulation(generator)

    val completeEntities = Timer("CompleteReferenceLinks") { CompleteReferenceLinks(referenceEntities, pool, population) }
    val fitnessFunction = config.fitnessFunction(completeEntities)
    
    updatePopulation(generator, targetFitness, completeEntities, fitnessFunction)

    //Select evaluation links
    updateStatus("Selecting evaluation links", 0.8)

    //TODO measure improvement of randomization
    selectLinks(generator, completeEntities, fitnessFunction)

    //Clean population
    if(referenceEntities.isDefined)
      population = executeSubTask(new CleanPopulationTask(population, fitnessFunction, generator))

    value.get
  }
  
  private def updatePool() = Timer("Generating Pool")  {
    //Build unlabeled pool
    if(pool.isEmpty) {
      updateStatus("Loading")
      pool = executeSubTask(new GeneratePoolTask(dataset, linkSpec, paths), 0.5)
    }

    //Assert that no reference links are in the pool
    pool = pool.filterNot(referenceEntities.positive.contains).filterNot(referenceEntities.negative.contains)
  }
  
  private def buildPopulation(generator: LinkageRuleGenerator) = Timer("Generating population") {
    if(population.isEmpty) {
      updateStatus("Generating population", 0.5)
      val seedRules = if(config.params.seed) linkSpec.rule :: Nil else Nil
      population = executeSubTask(new GeneratePopulationTask(seedRules, generator, config), 0.6, silent = true)
    }
  }
  
  private def updatePopulation(generator: LinkageRuleGenerator, targetFitness: Double, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double)) = Timer("Updating population") {
    for(i <- 0 until config.params.maxIterations
        if i > 0 || population.bestIndividual.fitness < targetFitness
        if LinkageRuleEvaluator(population.bestIndividual.node.build, completeEntities).fMeasure < config.params.destinationfMeasure) {
      val progress = 0.6 + 0.2 * (i + 1) / config.params.maxIterations
      population = executeSubTask(new ReproductionTask(population, fitnessFunction, generator, config), progress, silent = true)
      if(i % config.params.cleanFrequency == 0) {
        population = executeSubTask(new CleanPopulationTask(population, fitnessFunction, generator), progress, silent = true)
      }
    }
  }

  private def selectLinks(generator: LinkageRuleGenerator, completeEntities: ReferenceEntities, fitnessFunction: (LinkageRule => Double)) = Timer("Selecting links") {
    val randomizedPopulation = executeSubTask(new RandomizeTask(population, fitnessFunction, generator, config), 0.8, silent = true)

    val weightedRules = {
      val bestFitness = randomizedPopulation.bestIndividual.fitness
      val topIndividuals = randomizedPopulation.individuals.toSeq.filter(_.fitness >= bestFitness * 0.1).sortBy(-_.fitness)
      for(individual <- topIndividuals) yield {
        new WeightedLinkageRule(individual.node.build.operator, max(0.0001, individual.fitness))
      }
    }

    val valLinks = config.active.selector(weightedRules, pool.toSeq, completeEntities)
    value.update(valLinks)
  }
}

object ActiveLearningTask {
  def empty = new ActiveLearningTask(LearningConfiguration.default, Traversable.empty, LinkSpecification(), DPair.fill(Seq.empty), ReferenceEntities.empty)
}