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

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.learning.LearningConfiguration.Components
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.learning.individual.{AggregationNode, FunctionNode, LinkageRuleNode}
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.rule.similarity.DistanceMeasure
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.DPair

import scala.util.Random

case class LinkageRuleGenerator(comparisonGenerators: IndexedSeq[ComparisonGenerator], components: Components)
                               (implicit context: PluginContext) {
  //require(!comparisonGenerators.isEmpty, "comparisonGenerators must not be empty")

  def isEmpty: Boolean = comparisonGenerators.isEmpty
  
  private val aggregations = {
    val linear = if(components.linear) "average" :: Nil else Nil
    val boolean = if(components.boolean) "max" :: "min" :: Nil else Nil
    linear ++ boolean
  }
  
  /** The maximum weight of the generate aggregations */
  private val maxWeight = 20

  private val minOperatorCount = 1

  private val maxOperatorCount = 2

  /**
    * Generates a new random rule.
    */
  def apply(random: Random): LinkageRuleNode = {
    LinkageRuleNode(Some(generateAggregation(random)))
  }

  /**
    * Generate a node from an existing rule.
    */
  def load(rule: LinkageRule): LinkageRuleNode = {
    LinkageRuleNode.load(rule)
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
    val operators = {
      if(comparisonGenerators.nonEmpty) {
        for (i <- List.range(1, operatorCount + 1)) yield {
          generateComparison(random)
        }
      } else {
        List.empty
      }
    }

    //Build aggregation
    AggregationNode(
      aggregation = aggregation,
      weight = random.nextInt(maxWeight) + 1,
      operators = operators,
      isSingleValueAggregator = false
    )
  }

  private def generateComparison(random: Random) = {
    comparisonGenerators(random.nextInt(comparisonGenerators.size))(random)
  }
}

object LinkageRuleGenerator {

  def empty: LinkageRuleGenerator = {
    implicit val context: PluginContext = PluginContext.empty
    new LinkageRuleGenerator(IndexedSeq.empty, Components())
  }

  @deprecated
  def apply(entities: ReferenceEntities, components: Components = Components())
           (implicit context: PluginContext): LinkageRuleGenerator = {
    val es = entities.positiveEntities
    if(es.isEmpty)
      new LinkageRuleGenerator(IndexedSeq.empty, components)
    else {
      val paths = es.head.map(_.schema.typedPaths.map(_.toUntypedPath))
      new LinkageRuleGenerator((new CompatiblePathsGenerator(components).apply(entities, components.compatibleOnly) ++ new PatternGenerator(components).apply(paths)).toIndexedSeq, components)
    }
  }

  def apply(pathPairs: Seq[ComparisonPair],
            components: Components)
           (implicit context: PluginContext): LinkageRuleGenerator = {
    val generators =
      for {
        pathPair <- pathPairs
        generator <- createGenerators(pathPair.map(_.toUntypedPath), components)
      } yield generator

    new LinkageRuleGenerator(generators.toIndexedSeq, components)
  }

  private def createGenerators(pathPair: DPair[UntypedPath], components: Components)
                              (implicit context: PluginContext): Seq[ComparisonGenerator] = {
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("levenshteinDistance", Nil, DistanceMeasure), 3.0) ::
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("jaccard", Nil, DistanceMeasure), 1.0) ::
    // Substring is currently too slow ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("substring", Nil, DistanceMeasure), 0.6) ::
    ComparisonGenerator(InputGenerator.fromPathPair(pathPair, components.transformations), FunctionNode("date", Nil, DistanceMeasure), 1000.0) :: Nil
  }
}