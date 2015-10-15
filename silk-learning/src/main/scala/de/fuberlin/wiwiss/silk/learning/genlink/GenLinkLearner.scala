package de.fuberlin.wiwiss.silk.learning.genlink

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.learning._
import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result

case class GenLinkLearner(config: LearningConfiguration) extends LinkageRuleLearner {

  override def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): Activity[Result] = {
    new GenLink(trainingLinks, seeds, config)
  }
}