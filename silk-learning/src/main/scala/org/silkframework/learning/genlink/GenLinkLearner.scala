package org.silkframework.learning.genlink

import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.learning.LinkageRuleLearner.Result
import org.silkframework.learning.{LinkageRuleLearner, _}
import org.silkframework.rule.LinkageRule
import org.silkframework.runtime.activity.Activity

case class GenLinkLearner(config: LearningConfiguration) extends LinkageRuleLearner {

  override def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): Activity[Result] = {
    new GenLink(trainingLinks, seeds, config)
  }
}