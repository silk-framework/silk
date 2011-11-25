/* 
 * Copyright 2009-2011 Freie Universität Berlin
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

package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.datasource.Source
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Link, Path}
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.learning.reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.individual.{FitnessFunction, Population}
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}

//TODO support canceling
class ActiveLearningTask(config: LearningConfiguration,
                         sources: Traversable[Source],
                         linkSpec: LinkSpecification,
                         paths: DPair[Seq[Path]],
                         referenceEntities: ReferenceEntities = ReferenceEntities.empty,
                         var pool: Traversable[Link] = Traversable.empty,
                         var population: Population = Population()) extends ValueTask[Seq[Link]](Seq.empty) {

  def links = value.get

  override protected def execute(): Seq[Link] = {
    //Build unlabeled pool
    if(pool.isEmpty) {
      updateStatus("Loading")
      pool = executeSubTask(new GeneratePoolTask(sources, linkSpec, paths), 0.5)
    }

    //Build population
    val generator = LinkageRuleGenerator(ReferenceEntities.fromEntities(pool.map(_.entities.get), Nil), config.components)
    val targetFitness = if(population.isEmpty) 1.0 else population.bestIndividual.fitness

    if(population.isEmpty) {
      updateStatus("Generating population", 0.5)
      val seedRules = if(config.params.seed) linkSpec.rule :: Nil else Nil
      population = executeSubTask(new GeneratePopulationTask(seedRules, generator, config), 0.6, silent = true)
    }

    //Evolve population
    //TODO include CompleteReferenceLinks into fitness function
    val completeEntities = CompleteReferenceLinks(referenceEntities, pool, population)
    val fitnessFunction = new FitnessFunction(completeEntities, pool)

    for(i <- 0 until config.params.maxIterations
        if i > 0 || population.bestIndividual.fitness < targetFitness
        if LinkageRuleEvaluator(population.bestIndividual.node.build, completeEntities).fMeasure < config.params.destinationfMeasure) {
      val progress = 0.6 + 0.2 * (i + 1) / config.params.maxIterations
      population = executeSubTask(new ReproductionTask(population, fitnessFunction, generator, config), progress, silent = true)
      if(i % config.params.cleanFrequency == 0) {
        population = executeSubTask(new CleanPopulationTask(population, fitnessFunction, generator), progress, silent = true)
      }
    }

    //Sample links
    updateStatus("Sampling", 0.8)

    val valLinks = new SampleFromPopulationTask(population, pool.toSeq, completeEntities).apply()
    value.update(valLinks)

    //Clean population
    if(referenceEntities.isDefined)
      population = executeSubTask(new CleanPopulationTask(population, fitnessFunction, generator))

    valLinks
  }

  private def formatRule(rule: LinkageRule) = {
    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

    val paths = collectPaths(rule.operator.get)
    val shortPaths  = paths.map(_.serialize.split("[/#]").last.init)

    shortPaths.mkString(" ")
  }
}

object ActiveLearningTask {
  def empty = new ActiveLearningTask(LearningConfiguration.load(), Traversable.empty, LinkSpecification(), DPair.fill(Seq.empty), ReferenceEntities.empty)
}