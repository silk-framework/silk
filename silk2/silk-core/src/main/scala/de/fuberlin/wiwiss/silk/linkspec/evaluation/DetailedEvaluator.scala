package de.fuberlin.wiwiss.silk.linkspec.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.linkspec.condition.{Comparison, Aggregation, Operator, LinkCondition}
import de.fuberlin.wiwiss.silk.output.Link

object DetailedEvaluator
{
  def evaluate(condition : LinkCondition, instances : SourceTargetPair[Instance], threshold : Double) : Option[Link] =
  {
    val similarity = evaluateOperator(condition.rootOperator.get, instances, threshold)

    val confidence = similarity.similarity.getOrElse(0.0)

    if(confidence > threshold)
    {
      Some(new Link(instances.source.uri, instances.target.uri, confidence, Some(similarity)))
    }
    else
    {
      None
    }
  }

  def evaluateOperator(operator : Operator, instances : SourceTargetPair[Instance], threshold : Double) = operator match
  {
    case aggregation : Aggregation => evaluateAggregation(aggregation, instances, threshold)
    case comparison : Comparison => evaluateComparison(comparison, instances, threshold)
  }

  def evaluateAggregation(aggregation : Aggregation, instances : SourceTargetPair[Instance], threshold : Double) : Link.AggregatorSimilarity =
  {
    val totalWeights = aggregation.operators.map(_.weight).sum

    var isNone = false

    val operatorValues =
    {
      for(operator <- aggregation.operators) yield
      {
        val updatedThreshold = aggregation.aggregator.computeThreshold(threshold, operator.weight.toDouble / totalWeights)
        val value = evaluateOperator(operator, instances, updatedThreshold)
        if(operator.required && value.similarity.isEmpty) isNone = true

        value
      }
    }

    val weightedValues = aggregation.operators.map(_.weight) zip operatorValues.map(_.similarity.getOrElse(0.0))

    val aggregatedValue = aggregation.aggregator.evaluate(weightedValues)

    if(isNone)
      Link.AggregatorSimilarity(None, operatorValues)
    else
      Link.AggregatorSimilarity(aggregatedValue, operatorValues)
  }

  def evaluateComparison(comparision : Comparison, instances : SourceTargetPair[Instance], threshold : Double) : Link.ComparisonSimilarity =
  {
    val similarity = comparision.apply(instances, threshold)

    Link.ComparisonSimilarity(similarity, null, null)
  }
}