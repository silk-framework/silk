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

    for(instancePair <- instances.positive)
    {
      //val sourceIndices = linkCondition.index(instancePair.source, threshold)
      //val targetIndices = linkCondition.index(instancePair.target, threshold)

      if(//!sourceIndices.intersect(targetIndices).isEmpty &&
         linkCondition(instancePair, threshold) >= threshold)
      {
        truePositives += 1
      }
      else
      {
        falseNegatives += 1
      }
    }

    for(instancePair <- instances.negative)
    {
      //val sourceIndices = linkCondition.index(instancePair.source, threshold)
      //val targetIndices = linkCondition.index(instancePair.target, threshold)

      if(//!sourceIndices.intersect(targetIndices).isEmpty &&
         linkCondition(instancePair, threshold) >= threshold)
      {
        falsePositives += 1
      }
      else
      {
        trueNegatives += 1
      }
    }

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives)
  }
}