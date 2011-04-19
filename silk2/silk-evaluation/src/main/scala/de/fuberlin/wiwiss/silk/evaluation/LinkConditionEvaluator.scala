package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.condition.LinkCondition

object LinkConditionEvaluator
{
  //TODO remove threshold as soon as global threshold is removed
  def apply(linkCondition : LinkCondition, instances : ReferenceInstances, threshold : Double = 0.9) =
  {
    require(!instances.positive.isEmpty, "Positive alignment samples are required")
    require(!instances.negative.isEmpty, "Negative alignment samples are required")

    var truePositives : Int = 0
    var trueNegatives : Int = 0
    var falsePositives : Int = 0
    var falseNegatives : Int = 0
    var positiveScore : Double = instances.positive.size
    var negativeScore : Double = instances.negative.size

    for(instancePair <- instances.positive)
    {
      val confidence = linkCondition(instancePair, threshold)

      if(confidence >= threshold)
      {
        truePositives += 1
        positiveScore += (confidence - threshold) / (1.0 - threshold)
      }
      else
      {
        falseNegatives += 1
        positiveScore -= threshold - confidence / threshold
      }
    }

    for(instancePair <- instances.negative)
    {
      val confidence = linkCondition(instancePair, threshold)

      if(confidence >= threshold)
      {
        falsePositives += 1
        negativeScore -= (confidence - threshold) / (1.0 - threshold)
      }
      else
      {
        trueNegatives += 1
        negativeScore += threshold - confidence / threshold
      }
    }

    val score = 2.0 * positiveScore * negativeScore / (positiveScore + negativeScore)

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives, score)
  }
}