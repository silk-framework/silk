package org.silkframework.learning.active

import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.learning.generation.LinkageRuleGenerator
import org.silkframework.learning.individual.Population

/**
  * Holds the current state of the active learning workflow.
  *
  * @param pool The pool of known link candidates that are unlabeled.
  * @param comparisonPaths The matching paths from source and target.
  * @param generator The generator used for creating random linkage rules.
  * @param population The current population of learned linkage rules.
  * @param links Link candidates from the unlabeled pool that have been selected for manual confirmation by the user.
  */
case class ActiveLearningState(comparisonPaths: Seq[ComparisonPair],
                               referenceData: ActiveLearningReferenceData,
                               generator: LinkageRuleGenerator,
                               population: Population,
                               links: Seq[LinkCandidate],
                               randomSeed: Long)

object ActiveLearningState {
  def initial(randomSeed: Long): ActiveLearningState = {
    ActiveLearningState(Seq.empty, ActiveLearningReferenceData.empty, LinkageRuleGenerator.empty, Population.empty, Seq.empty, randomSeed = randomSeed)
  }
}
