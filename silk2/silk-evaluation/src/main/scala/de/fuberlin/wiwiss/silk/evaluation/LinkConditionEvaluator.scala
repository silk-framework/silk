package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.condition.LinkCondition

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

    var positiveError = 0.0
    var negativeError = 0.0

    for(instancePair <- instances.positive.values)
    {
      val confidence = linkCondition(instancePair, -1.0)

      if(confidence >= 0.0)
      {
        truePositives += 1
      }
      else
      {
        falseNegatives += 1
        positiveError += -confidence
      }
    }

    for(instancePair <- instances.negative.values)
    {
      val confidence = linkCondition(instancePair, -1.0)

      if(confidence >= 0.0)
      {
        falsePositives += 1
        negativeError += confidence
      }
      else
      {
        trueNegatives += 1
      }
    }

    val score =
    {
      val positiveScore = 1.0 - positiveError / instances.positive.size
      val negativeScore = 1.0 - negativeError / instances.negative.size

      if(positiveScore + negativeScore == 0.0)
      {
        0.0
      }
      else
      {
        2.0 * positiveScore * negativeScore / (positiveScore + negativeScore)
      }
    }

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives, score)
  }
}