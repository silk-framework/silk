package de.fuberlin.wiwiss.silk.learning.generation

import util.Random
import de.fuberlin.wiwiss.silk.impl.metric.LevenshteinMetric
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.input.{PathInput, Transformer}
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.learning.individual._

object RandomGenerator {
  /**
   * Creates a new random link condition.
   */
  def apply(config: GenerationConfiguration): LinkConditionNode = {
    LinkConditionNode(Some(generateAggregation(config)))
  }

  val aggregations = "max" :: "min" :: "average" :: Nil

  val minOperatorCount = 1

  val maxOperatorCount = 2

  /**
   * Generates a random aggregation node.
   */
  def generateAggregation(config: GenerationConfiguration): AggregationNode = {
    //Choose a random aggregation
    val aggregation = aggregations(Random.nextInt(aggregations.size))

    //Choose a random operator count
    val operatorCount = minOperatorCount + Random.nextInt(maxOperatorCount - minOperatorCount + 1)

    //Generate operators
    val operators =
      for (i <- List.range(1, operatorCount + 1)) yield {
        generateComparison(config)
      }

    //Build aggregation
    new AggregationNode(aggregation, operators)
  }

  val specialCaseProbability = 0.005

  val metric = new LevenshteinMetric()

  //TODO: Make config an implicit parameter
  def generateComparison(config: GenerationConfiguration): ComparisonNode = {
//    if (Random.nextDouble <= specialCaseProbability) {
//      for (comparison <- ComparisonGenerator.generate(config)) return comparison
//    }

    val pathPairs = config.pathPairs.toIndexedSeq
    val pathPair = pathPairs(Random.nextInt(pathPairs.size))

    val sourceInput = generateInput(pathPair.source, true)
    val targetInput = generateInput(pathPair.target, false)

    ComparisonNode(SourceTargetPair(sourceInput, targetInput), Random.nextInt(5), StrategyNode("levenshteinDistance", Nil, DistanceMeasure))
  }

  val transformationProbability = 0.5

  val transformers = "lowerCase" :: "stripUriPrefix" :: Nil

  def generateInput(path: Path, isSource: Boolean): InputNode = {
    val pathInput = new PathInputNode(isSource, path)

    if(Random.nextDouble < transformationProbability) {
      val transformer = transformers(Random.nextInt(transformers.size))
      TransformNode(isSource, pathInput :: Nil, StrategyNode(transformer, Nil, Transformer))
    } else {
      pathInput
    }
  }
}