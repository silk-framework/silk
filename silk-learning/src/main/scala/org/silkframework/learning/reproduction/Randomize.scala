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
import org.silkframework.learning.individual.Population
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}

import scala.util.Random

/**
 * Randomizes the population by mutating its individuals.
 */
class Randomize(population: Population,
                fitnessFunction: (LinkageRule => Double),
                generator: LinkageRuleGenerator,
                config: LearningConfiguration,
                randomSeed: Long) extends Activity[Population] {

  private val mutation = new MutationFunction(new CrossoverFunction(fitnessFunction, config.components), generator)

  override def run(context: ActivityContext[Population])
                  (implicit userContext: UserContext): Unit = {
    // Generate a separate random seed for each individual
    val random = new Random(randomSeed)
    val randomSeeds = Seq.fill(population.individuals.size)(random.nextLong())

    // Generate new individuals
    val updatedIndividuals = for((individual, randomSeed) <- (population.individuals zip randomSeeds).par) yield mutation(individual, new Random(randomSeed))
    context.value.update(Population(updatedIndividuals.seq))
  }
}