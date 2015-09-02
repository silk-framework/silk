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

package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components
import de.fuberlin.wiwiss.silk.util.DPair
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, LinkageRuleNode}
import de.fuberlin.wiwiss.silk.rule.LinkageRule

/**
 * Combines two linkage rules into a new one.
 */
class CrossoverFunction(fitnessFunction: (LinkageRule => Double), components: Components) extends ((Individual, Individual) => Individual) {
  /**
   * The operators which will be employed for crossover.
   */
  val operators = {
    if(components.useSpecializedCrossover) {
      var ops = List[CrossoverOperator]()

      //We always learn thresholds and weights
      ops ::= ThresholdCrossover()
      ops ::= WeightCrossover()
      ops ::= RequiredCrossover()
      //We always modify existing aggregations
      ops ::= AggregationOperatorsCrossover()
      ops ::= AggregationFunctionCrossover()
      //We always learn distance functions
      ops ::= DistanceMeasureCrossover()

      if(components.transformations)
        ops ::= TransformationCrossover()

      if(components.hierarchies)
        ops ::= OperatorCrossover()

      ops
    }
    else {
      SubtreeCrossover() :: Nil
    }
  }

  /**
   * Combines two linkage rules into a new one.
   */
  def apply(ind1: Individual, ind2: Individual): Individual = {
    //Choose a random crossover operator
    val operator = operators(Random.nextInt(operators.size))

    //Apply operator
    val combined =
      operator(DPair(ind1.node, ind2.node)) match {
      case Some(resultNode) => {
        resultNode
      }
      case None => {
        //No compatible pairs for this operator found => return unmodified node
        ind1.node
      }
    }

    Individual(combined, fitnessFunction(combined.build))
  }
}