package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.condition.LinkCondition
import scala.math.pow

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

    var positiveError = 0.0
    var negativeError = 0.0

    for(instancePair <- instances.positive.values)
    {
      val confidence = linkCondition(instancePair, threshold)

      if(confidence >= threshold)
      {
        truePositives += 1
        positiveScore += pow((confidence - threshold) / (1.0 - threshold), 2.0)
      }
      else
      {
        falseNegatives += 1
        positiveScore -= pow(threshold - confidence / threshold, 2.0)
        positiveError += pow(threshold - confidence / threshold, 2.0)
      }
    }

    for(instancePair <- instances.negative.values)
    {
      val confidence = linkCondition(instancePair, threshold)

      if(confidence >= threshold)
      {
        falsePositives += 1
        negativeScore -= pow((confidence - threshold) / (1.0 - threshold), 2.0)
        negativeError += pow((confidence - threshold) / (1.0 - threshold), 2.0)
      }
      else
      {
        trueNegatives += 1
        negativeScore += pow(threshold - confidence / threshold, 2.0)
      }
    }

    //val score = -error//1.0 - error / (instances.positive.size + instances.negative.size)

    positiveError = 1.0 - positiveError / instances.positive.size
    negativeError = 1.0 - negativeError / instances.negative.size

    val error = 2.0 * positiveError * negativeError / (positiveError + negativeError)
    val score = error

    //val score = 2.0 * positiveScore * negativeScore / (positiveScore + negativeScore)

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives, score)
  }
}