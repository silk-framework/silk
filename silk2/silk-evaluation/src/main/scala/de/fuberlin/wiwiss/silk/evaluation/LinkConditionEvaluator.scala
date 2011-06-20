package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.condition.LinkCondition
import scala.math.pow

object LinkConditionEvaluator
{
  def apply(linkCondition : LinkCondition, instances : ReferenceInstances) =
  {
    require(!instances.positive.isEmpty, "Positive alignment samples are required")
    require(!instances.negative.isEmpty, "Negative alignment samples are required")

    var truePositives : Int = 0
    var trueNegatives : Int = 0
    var falsePositives : Int = 0
    var falseNegatives : Int = 0

    var positiveScore : Double = instances.positive.size
    var negativeScore : Double = instances.negative.size

    var positiveError = 0.0
    var negativeError = 0.0

    for(instancePair <- instances.positive.values)
    {
      val confidence = linkCondition(instancePair, -1.0)

      if(confidence >= 0.0)
      {
        truePositives += 1
        positiveScore += pow(confidence, 2.0)
      }
      else
      {
        falseNegatives += 1
        positiveScore -= pow(confidence, 2.0)
        positiveError += pow(confidence, 2.0)
      }
    }

    for(instancePair <- instances.negative.values)
    {
      val confidence = linkCondition(instancePair, -1.0)

      if(confidence >= 0.0)
      {
        falsePositives += 1
        negativeScore -= pow(confidence, 2.0)
        negativeError += pow(confidence, 2.0)
      }
      else
      {
        trueNegatives += 1
        negativeScore += pow(confidence, 2.0)
      }
    }

    //val score = -error//1.0 - error / (instances.positive.size + instances.negative.size)

    val score =
    {
      val positive = 1.0 - positiveError / instances.positive.size
      val negative = 1.0 - negativeError / instances.negative.size

      if(positive + negative == 0.0)
      {
        0.0
      }
      else
      {
        2.0 * positive * negative / (positive + negative)
      }
    }

    //val score = 2.0 * positiveScore * negativeScore / (positiveScore + negativeScore)

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives, score)
  }
}