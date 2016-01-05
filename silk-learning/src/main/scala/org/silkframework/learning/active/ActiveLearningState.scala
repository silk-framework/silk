package org.silkframework.learning.active

import org.silkframework.entity.Link
import org.silkframework.learning.generation.LinkageRuleGenerator
import org.silkframework.learning.individual.Population

/**
  * Holds the current state of the active learning workflow.
  *
  * @param pool The pool of known link candidates that are unlabeled.
  * @param generator The generator used for creating random linkage rules.
  * @param population The current population of learned linkage rules.
  * @param links Link candidates from the unlabeled pool that have been selected for manual confirmation by the user.
  */
case class ActiveLearningState(pool: UnlabeledLinkPool, generator: LinkageRuleGenerator, population: Population, links: Seq[Link])

object ActiveLearningState {
  def initial = ActiveLearningState(UnlabeledLinkPool.empty, LinkageRuleGenerator.empty, Population.empty, Seq.empty)
}
