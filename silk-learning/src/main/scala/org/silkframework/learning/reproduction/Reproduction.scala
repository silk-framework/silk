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
import org.silkframework.runtime.activity.{Activity, ActivityContext}

import scala.util.Random

class Reproduction(population: Population,
                   fitnessFunction: (LinkageRule => Double),
                   generator: LinkageRuleGenerator,
                   config: LearningConfiguration) extends Activity[Population] {

  private val individuals = population.individuals.toArray

  private val crossover = new CrossoverFunction(fitnessFunction, config.components)

  private val mutation = new MutationFunction(crossover, generator)

  override def run(context: ActivityContext[Population]): Unit = {
    //Get the best individuals and recompute their fitness as the reference links may have changed
    val elite = individuals.sortBy(-_.fitness)
                           .take(config.reproduction.elitismCount)
                           .map(i => i.copy(fitness = fitnessFunction(i.node.build)))

    //Number of individuals to be generated
    val count = individuals.size - config.reproduction.elitismCount

    val offspring = for(i <- (0 until count).par) yield reproduce()

    context.value.update(Population(elite ++ offspring))
  }

  private def reproduce(): Individual = {
    if(Random.nextDouble < config.reproduction.mutationProbability)
      mutation(select())
    else
      crossover(select(), select())
  }

  private def select(): Individual = {
    val tournamentNodes = List.fill(config.reproduction.tournamentSize)(individuals(Random.nextInt(individuals.size)))

    tournamentNodes.reduceLeft((n1, n2) => if (n1.fitness > n2.fitness) n1 else n2)
  }
}