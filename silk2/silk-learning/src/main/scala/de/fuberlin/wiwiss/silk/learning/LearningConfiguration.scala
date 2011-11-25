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

package de.fuberlin.wiwiss.silk.learning

import reproduction.{ReproductionConfiguration}
import xml.XML
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration._

case class LearningConfiguration(components: Components, reproduction: ReproductionConfiguration, params: Parameters)

object LearningConfiguration {

  val defaultConfigFile = "de/fuberlin/wiwiss/silk/learning/config.xml"

  def empty = load()

  def load() = {

    //val xml = XML.load(getClass.getClassLoader.getResourceAsStream(defaultConfigFile))

    LearningConfiguration(
      components = Components(),
      reproduction = ReproductionConfiguration(),
      params = Parameters()
    )
  }

  case class Components(transformations: Boolean = true, aggregations: Boolean = true)

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