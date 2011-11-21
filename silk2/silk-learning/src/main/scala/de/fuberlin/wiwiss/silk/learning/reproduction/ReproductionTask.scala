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

package de.fuberlin.wiwiss.silk.learning.reproduction

import util.Random
import de.fuberlin.wiwiss.silk.util.{ParallelMapper, DPair}
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, Population}
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

class ReproductionTask(population: Population, fitnessFunction: (LinkageRule => Double), generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  /**
   * The operators which will be employed for crossover.
   */
  private val crossoverOperators = {
    var operators = List[CrossoverOperator]()

    //We always learn thresholds and weights
    operators ::= ThresholdCrossover()
    operators ::= WeightCrossover()

    if(config.components.transformations) {
      operators ::= TransformationCrossover()
    }

    if(config.components.aggregations) {
      operators ::= AggregationOperatorsCrossover()
      operators ::= AggregationStrategyCrossover()
      operators ::= OperatorCrossover()
    }

    operators
  }

  private val individuals = population.individuals.toArray

  override def execute(): Population = {
    //Get the best individuals and recompute their fitness as the reference links may have changed
    val elite = individuals.sortBy(-_.fitness)
                           .take(config.reproduction.elitismCount)
                           .map(i => i.copy(fitness = fitnessFunction(i.node.build)))

    //Number of individuals to be generated
    val count = individuals.size - config.reproduction.elitismCount

    val offspring = new ParallelMapper(0 until count).map {
      i => updateStatus(i.toDouble / count); reproduce()
    }

    Population(elite ++ offspring)
  }

  private def reproduce(): Individual = {
    //Choose a random crossover operator
    val operator = crossoverOperators(Random.nextInt(crossoverOperators.size))

    //Define the two crossover individuals: In case of mutation, we do a crossover with a new random node
    val sourceIndividual = select()
    val targetLinkageRule = if (Random.nextDouble < config.reproduction.mutationProbability) generator() else select().node

    val node =
      operator(DPair(sourceIndividual.node, targetLinkageRule)) match {
        case Some(resultNode) => {
          resultNode
        }
        case None => {
          //No compatible pairs for this operator found => return unmodified node
          sourceIndividual.node
        }
      }

    Individual(node, fitnessFunction(node.build))
  }

  private def select(): Individual = {
    val tournamentNodes = List.fill(config.reproduction.tournamentSize)(individuals(Random.nextInt(individuals.size)))

    tournamentNodes.reduceLeft((n1, n2) => if (n1.fitness > n2.fitness) n1 else n2)
  }
}