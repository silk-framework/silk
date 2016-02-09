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

package org.silkframework.learning

import org.silkframework.learning.LearningConfiguration._
import org.silkframework.learning.active.ActiveLearningConfiguration
import org.silkframework.learning.individual.fitness.{FitnessFunction, MCCFitnessFunction}
import org.silkframework.learning.reproduction.ReproductionConfiguration

case class LearningConfiguration(name: String = "Unamed",
                                 components: Components = Components(),
                                 active: ActiveLearningConfiguration = ActiveLearningConfiguration(),
                                 reproduction: ReproductionConfiguration = ReproductionConfiguration(),
                                 fitnessFunction: FitnessFunction = MCCFitnessFunction(),
                                 params: Parameters = Parameters())

object LearningConfiguration {

  def default = LearningConfiguration()

  /**
   * Specifies which components of the linkage rules are learned.
   * 
   * @param compatibleOnly Only generate linkage rules with compatible path pairs
   * @param linear Include linear aggregations
   * @param boolean Include boolean aggregations
   * @param hierarchies Build aggregation hierarchies
   * @param transformations Use transformations
   * @param useSpecializedCrossover Use specialized crossover operations
   */
  case class Components(compatibleOnly: Boolean = true,
                        linear: Boolean = true,
                        boolean: Boolean = true,
                        hierarchies: Boolean = true,
                        transformations: Boolean = true,
                        useSpecializedCrossover: Boolean = true)

  /**
   * The parameters of the learning algorithm.
   *
   * @param seed Seed the population with the current linkage rule.
   * @param populationSize The size of the population.
   * @param maxIterations The maximum number of iterations before giving up.
   * @param maxIneffectiveIterations The maximum number of subsequent iterations without any increase in fitness before giving up.
   * @param cleanFrequency The number of iterations between two runs of the cleaning algorithm.
   * @param destinationfMeasure The desired fMeasure. The algorithm will stop after reaching it.
   */
  case class Parameters(seed: Boolean = true,
                        populationSize: Int = 500,
                        maxIterations: Int = 50,
                        maxIneffectiveIterations: Int = 50,
                        cleanFrequency: Int = 5,
                        destinationfMeasure: Double = 0.999)
}