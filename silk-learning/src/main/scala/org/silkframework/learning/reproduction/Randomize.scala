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
import org.silkframework.runtime.activity.{Activity, ActivityContext}

/**
 * Randomizes the population by mutating its individuals.
 */
class Randomize(population: Population,
                fitnessFunction: (LinkageRule => Double),
                generator: LinkageRuleGenerator,
                config: LearningConfiguration) extends Activity[Population] {

  private val mutation = new MutationFunction(new CrossoverFunction(fitnessFunction, config.components), generator)

  override def run(context: ActivityContext[Population]): Unit = {
    context.value.update(Population(population.individuals.par.map(mutation).seq))
  }
}