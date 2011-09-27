package de.fuberlin.wiwiss.silk.learning.generation

import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, AggregationNode}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.Components
import de.fuberlin.wiwiss.silk.learning.individual.FunctionNode._
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.util.DPair

class LinkageRuleGenerator(comparisonGenerators: IndexedSeq[ComparisonGenerator], components: Components) {

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

  def apply(paths: DPair[Traversable[Path]], components: Components) = {
    new LinkageRuleGenerator((new FullGenerator(components).apply(paths) ++ new PatternGenerator(components).apply(paths)).toIndexedSeq, components)
  }

  def apply(entities: ReferenceEntities, components: Components) = {
    val paths = entities.positive.values.head.map(_.desc.paths)

    new LinkageRuleGenerator((new PathPairGenerator(components).apply(entities) ++ new PatternGenerator(components).apply(paths)).toIndexedSeq, components)
  }
}