package de.fuberlin.wiwiss.silk.learning.genlink

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.learning._
import de.fuberlin.wiwiss.silk.runtime.oldtask.ValueTask
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result

case class GenLink(config: LearningConfiguration) extends LinkageRuleLearner {

  override def learn(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule]): ValueTask[Result] = {
    new GenLinkTask(trainingLinks, seeds, config)
  }
}


