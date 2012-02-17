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

package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.DPair
import util.Random
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.learning.individual._

/**
 * Generates random comparisons.
 */
class ComparisonGenerator(inputGenerators: DPair[InputGenerator], measure: FunctionNode[DistanceMeasure], maxThreshold: Double) {

  /** The maximum weight of the generate comparisons */
  val maxWeight = 20

  /**
   * Generates a new comparison.
   */
  def apply() = {
    ComparisonNode(
      inputs = inputGenerators.map(_.apply()),
      threshold =  Random.nextDouble() * maxThreshold,
      weight = Random.nextInt(maxWeight) + 1,
      required = Random.nextBoolean(),
      metric = measure
    )
  }
}