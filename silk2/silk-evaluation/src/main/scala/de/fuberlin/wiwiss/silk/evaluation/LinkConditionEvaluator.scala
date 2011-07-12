package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.condition.LinkCondition
import math.sqrt

object LinkConditionEvaluator
{
  def apply(linkCondition : LinkCondition, instances : ReferenceInstances) =
  {
    require(!instances.positive.isEmpty, "Positive alignment samples are required")
    require(!instances.negative.isEmpty, "Negative alignment samples are required")

    val startTime = System.currentTimeMillis()

    var truePositives : Int = 0
    var trueNegatives : Int = 0
    var falsePositives : Int = 0
    var falseNegatives : Int = 0

    var positiveScore = 0.0
    var negativeScore = 0.0
    var positiveError = 0.0
    var negativeError = 0.0

    for(instancePair <- instances.positive.values)
    {
      val confidence = linkCondition(instancePair, 0.0)

      if(confidence >= 0.0)
      {
        truePositives += 1
        positiveScore += confidence
      }
      else
      {
        falseNegatives += 1
        positiveError += -confidence
      }
    }

    for(instancePair <- instances.negative.values)
    {
      val confidence = linkCondition(instancePair, 0.0)

      if(confidence >= 0.0)
      {
        falsePositives += 1
        negativeError += confidence
      }
      else
      {
        trueNegatives += 1
        negativeScore += -confidence
      }
    }

//    val score =
//    {
//      val positiveScore = 1.0 - positiveError / instances.positive.size
//      val negativeScore = 1.0 - negativeError / instances.negative.size
//
//      if(positiveScore + negativeScore == 0.0)
//      {
//        0.0
//      }
//      else
//      {
//        2.0 * positiveScore * negativeScore / (positiveScore + negativeScore)
//      }
//    }

//    val score =
//    {
//      val cross = positiveScore * negativeScore - negativeError * positiveError
//      val sum = (positiveScore + negativeError) * (positiveScore + positiveError) * (negativeScore + negativeError) * (negativeScore + positiveError)
//
//      if(sum != 0.0) cross.toDouble / sqrt(sum.toDouble) else 0.0
//    }

    val time = System.currentTimeMillis() - startTime
    println("XXX " + time + linkCondition.toString)

    val score =
    {
      (positiveScore / (positiveScore + positiveError)) * (negativeScore / (negativeScore + negativeError))
    }

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives)
  }
}