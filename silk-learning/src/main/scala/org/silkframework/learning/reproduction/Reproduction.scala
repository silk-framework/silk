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

package org.silkframework.learning.reproduction

import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.generation.LinkageRuleGenerator
import org.silkframework.learning.individual.{Individual, Population}
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.RandomUtils

import scala.collection.parallel.ParSeq
import scala.util.Random

class Reproduction(population: Population,
                   fitnessFunction: (LinkageRule => Double),
                   generator: LinkageRuleGenerator,
                   config: LearningConfiguration,
                   randomSeed: Long) extends Activity[Population] {

  private val individuals = population.individuals.toArray

  private val crossover = new CrossoverFunction(fitnessFunction, config.components)

  private val mutation = new MutationFunction(crossover, generator)

  override def run(context: ActivityContext[Population])
                  (implicit userContext: UserContext): Unit = {
    //Get the best individuals and recompute their fitness as the reference links may have changed
    val elite = individuals.sortBy(-_.fitness)
                           .take(config.reproduction.elitismCount)
                           .map(i => i.copy(fitness = fitnessFunction(i.node.build)))

    //Number of individuals to be generated
    val count = individuals.length - config.reproduction.elitismCount
    val offspring = for(random <- RandomUtils.randomSeq(count, randomSeed).par) yield reproduce(random)

    context.value.update(Population(elite ++ offspring))
  }

  private def reproduce(random: Random): Individual = {
    if(random.nextDouble < config.reproduction.mutationProbability)
      mutation(select(random), random)
    else
      crossover(select(random), select(random), random)
  }

  private def select(random: Random): Individual = {
    val tournamentNodes = List.fill(config.reproduction.tournamentSize)(individuals(random.nextInt(individuals.length)))

    tournamentNodes.reduceLeft((n1, n2) => if (n1.fitness > n2.fitness) n1 else n2)
  }
}