/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.rule.LinkageRule

object LinkageRuleEvaluator {
  def apply(rule: LinkageRule, entity: ReferenceEntities, threshold: Double = 0.0): EvaluationResult = {
    var truePositives: Int = 0
    var trueNegatives: Int = 0
    var falsePositives: Int = 0
    var falseNegatives: Int = 0

    var positiveScore = 0.0
    var negativeScore = 0.0
    var positiveError = 0.0
    var negativeError = 0.0

    for (entityPair <- entity.positive.values) {
      val confidence = rule(entityPair, threshold)

      if (confidence >= threshold) {
        truePositives += 1
        positiveScore += confidence
      }
      else {
        falseNegatives += 1
        positiveError += -confidence
      }
    }

    for (entityPair <- entity.negative.values) {
      val confidence = rule(entityPair, threshold)

      if (confidence >= threshold) {
        falsePositives += 1
        negativeError += confidence
      }
      else {
        trueNegatives += 1
        negativeScore += -confidence
      }
    }

    //    val score =
    //    {
    //      val positiveScore = 1.0 - positiveError / entities.positive.size
    //      val negativeScore = 1.0 - negativeError / entities.negative.size
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

//        val score =
//        {
//          val cross = positiveScore * negativeScore - negativeError * positiveError
//          val sum = (positiveScore + negativeError) * (positiveScore + positiveError) * (negativeScore + negativeError) * (negativeScore + positiveError)
//
//          if(sum != 0.0) cross.toDouble / math.sqrt(sum.toDouble) else 0.0
//        }
//
//    val score = {
//      (positiveScore / (positiveScore + positiveError)) * (negativeScore / (negativeScore + negativeError))
//    }

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives)
  }
}