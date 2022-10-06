package org.silkframework.rule.evaluation

import org.silkframework.entity.{LinkDecision, ReferenceLink}

import java.util.logging.Logger
import org.silkframework.rule.LinkageRule

object LinkageRuleEvaluator {
  val log: Logger = Logger.getLogger(this.getClass.getName)

  def apply(rule: LinkageRule,
            referenceLinks: Seq[ReferenceLink],
            threshold: Double): EvaluationResult = {
    var truePositives: Int = 0
    var trueNegatives: Int = 0
    var falsePositives: Int = 0
    var falseNegatives: Int = 0
    referenceLinks.foreach(link => {
      val positive = link.decision == LinkDecision.POSITIVE
      val negative = link.decision == LinkDecision.NEGATIVE
      val confidence = rule(link.linkEntities, threshold)
      if(positive || negative) {
        if(confidence >= threshold) {
          if(positive) {
            truePositives += 1
          } else {
            falsePositives += 1
          }
        } else {
          if(negative) {
            trueNegatives += 1
          } else {
            falseNegatives += 1
          }
        }
      }
    })
    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives)
  }

  def apply(rule: LinkageRule,
            entity: ReferenceEntities,
            threshold: Double = 0.0,
            logFalseNegatives: Boolean = false,
            logFalsePositives: Boolean = false): EvaluationResult = {
    var truePositives: Int = 0
    var trueNegatives: Int = 0
    var falsePositives: Int = 0
    var falseNegatives: Int = 0

    for (entityPair <- entity.positiveEntities) {
      val confidence = rule(entityPair, threshold)

      if (confidence >= threshold) {
        truePositives += 1
      }
      else {
        falseNegatives += 1
        if(logFalseNegatives) {
          log.warning("False Negative: " + entityPair)
        }
      }
    }

    for (entityPair <- entity.negativeEntities) {
      val confidence = rule(entityPair, threshold)

      if (confidence >= threshold) {
        falsePositives += 1
        if(logFalsePositives) {
          log.warning("False Positive: " + entityPair)
        }
      }
      else {
        trueNegatives += 1
      }
    }

    new EvaluationResult(truePositives, trueNegatives, falsePositives, falseNegatives)
  }
}
