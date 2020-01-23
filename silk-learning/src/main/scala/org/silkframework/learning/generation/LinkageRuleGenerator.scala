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

package org.silkframework.learning.generation

import org.silkframework.learning.LearningConfiguration.Components
import org.silkframework.learning.individual.{AggregationNode, LinkageRuleNode}
import org.silkframework.rule.evaluation.ReferenceEntities

import scala.util.Random

case class LinkageRuleGenerator(comparisonGenerators: IndexedSeq[ComparisonGenerator], components: Components) {
  //require(!comparisonGenerators.isEmpty, "comparisonGenerators must not be empty")
  
  private val aggregations = {
    val linear = if(components.linear) "average" :: Nil else Nil
    val boolean = if(components.boolean) "max" :: "min" :: Nil else Nil
    linear ++ boolean
  }
  
  /** The maximum weight of the generate aggregations */
  private val maxWeight = 20

  private val minOperatorCount = 1

  private val maxOperatorCount = 2

  def apply(random: Random): LinkageRuleNode = {
    LinkageRuleNode(Some(generateAggregation(random)))
  }

  /**
   * Generates a random aggregation node.
   */
  private def generateAggregation(random: Random): AggregationNode = {
    //Choose a random aggregation
    val aggregation = aggregations(random.nextInt(aggregations.size))

    //Choose a random operator count
    val operatorCount = minOperatorCount + random.nextInt(maxOperatorCount - minOperatorCount + 1)

    //Generate operators
    val operators =
      for (i <- List.range(1, operatorCount + 1)) yield {
        generateComparison(random)
      }

    //Build aggregation
    AggregationNode(
      aggregation = aggregation,
      weight = random.nextInt(maxWeight) + 1,
      required = random.nextBoolean(),
      operators = operators
    )
  }

  private def generateComparison(random: Random) = {
    comparisonGenerators(random.nextInt(comparisonGenerators.size))(random)
  }
}

object LinkageRuleGenerator {

  def empty: LinkageRuleGenerator = {
    new LinkageRuleGenerator(IndexedSeq.empty, Components())
  }

  def apply(entities: ReferenceEntities, components: Components = Components()): LinkageRuleGenerator = {
    val es = entities.positiveEntities
    if(es.isEmpty)
      new LinkageRuleGenerator(IndexedSeq.empty, components)
    else {
      val paths = es.head.map(_.schema.typedPaths.map(_.toUntypedPath))
      new LinkageRuleGenerator((new CompatiblePathsGenerator(components).apply(entities, components.compatibleOnly) ++ new PatternGenerator(components).apply(paths)).toIndexedSeq, components)
    }
  }
}