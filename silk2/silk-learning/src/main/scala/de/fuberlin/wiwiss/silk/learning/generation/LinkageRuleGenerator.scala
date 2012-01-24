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

import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, AggregationNode}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components

class LinkageRuleGenerator(comparisonGenerators: IndexedSeq[ComparisonGenerator], components: Components) {
  require(!comparisonGenerators.isEmpty, "comparisonGenerators most not be empty")

  private val aggregations = "max" :: "min" :: "average" :: Nil

  private val minOperatorCount = 1

  private val maxOperatorCount = 2

  def apply() = {
    if(components.aggregations) {
      LinkageRuleNode(Some(generateAggregation()))
    } else {
      LinkageRuleNode(Some(generateComparison()))
    }
  }

  /**
   * Generates a random aggregation node.
   */
  private def generateAggregation(): AggregationNode = {
    //Choose a random aggregation
    val aggregation = aggregations(Random.nextInt(aggregations.size))

    //Choose a random operator count
    val operatorCount = minOperatorCount + Random.nextInt(maxOperatorCount - minOperatorCount + 1)

    //Generate operators
    val operators =
      for (i <- List.range(1, operatorCount + 1)) yield {
        generateComparison()
      }

    //Build aggregation
    new AggregationNode(aggregation, operators)
  }

  private def generateComparison() = {
    comparisonGenerators(Random.nextInt(comparisonGenerators.size))()
  }
}

object LinkageRuleGenerator {

  def apply(entities: ReferenceEntities, components: Components = Components()) = {
    //TODO assert that the empty paths are included
    if(entities.positive.isEmpty)
      new LinkageRuleGenerator(IndexedSeq.empty, components)
    else {
      val paths = entities.positive.values.head.map(_.desc.paths)
      new LinkageRuleGenerator((new CompatiblePathsGenerator(components).apply(entities, components.seed) ++ new PatternGenerator(components).apply(paths)).toIndexedSeq, components)
    }
  }
}