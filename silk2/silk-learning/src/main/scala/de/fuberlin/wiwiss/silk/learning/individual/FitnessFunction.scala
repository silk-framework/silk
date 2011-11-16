package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.util.DPair

class FitnessFunction(referenceEntities: ReferenceEntities,
                      unlabeledLinks: Traversable[Link]) extends (LinkageRule => Double) {

  def apply(linkageRule: LinkageRule) = {
    if(referenceEntities.isDefined) {
      LinkageRuleEvaluator(linkageRule, referenceEntities).mcc
    }
    else if(filter(linkageRule)) {
      1.0
    }
    else {
      0.0
    }
  }

  private def filter(linkageRule: LinkageRule) = {
    val entityPairs = unlabeledLinks.toSeq.map(_.entities.get)
    val shuffledEntityPairs = for((s, t) <- entityPairs.map(_.source) zip (entityPairs.tail.map(_.target) :+ entityPairs.head.target)) yield DPair(s, t)

    val count = (entityPairs ++ shuffledEntityPairs).filter(linkageRule(_) > 0.0).size

    count > 0 && count <= unlabeledLinks.size
  }
}