package org.silkframework.learning.active

import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.generation.LinkageRuleGenerator
import org.silkframework.learning.individual.Population
import org.silkframework.util.DPair

/**
  * Holds the current state of the active learning workflow.
  *
  * @param pool The pool of known link candidates that are unlabeled.
  * @param comparisonPaths The matching paths from source and target.
  * @param generator The generator used for creating random linkage rules.
  * @param population The current population of learned linkage rules.
  * @param links Link candidates from the unlabeled pool that have been selected for manual confirmation by the user.
  */
case class ActiveLearningState(pool: UnlabeledLinkPool,
                               comparisonPaths: Seq[DPair[TypedPath]],
                               generator: LinkageRuleGenerator,
                               population: Population,
                               links: Seq[LinkCandidate],
                               randomSeed: Long)

object ActiveLearningState {
  def initial(randomSeed: Long): ActiveLearningState = {
    ActiveLearningState(UnlabeledLinkPool.empty, Seq.empty, LinkageRuleGenerator.empty, Population.empty, Seq.empty, randomSeed = randomSeed)
  }
}
