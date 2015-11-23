package org.silkframework.learning.genlink

import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.learning.LinkageRuleLearner
import org.silkframework.rule.LinkageRule
import org.silkframework.learning._
import org.silkframework.runtime.activity.Activity
import LinkageRuleLearner.Result

case class GenLinkLearner(config: LearningConfiguration) extends LinkageRuleLearner {

  override def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): Activity[Result] = {
    new GenLink(trainingLinks, seeds, config)
  }
}