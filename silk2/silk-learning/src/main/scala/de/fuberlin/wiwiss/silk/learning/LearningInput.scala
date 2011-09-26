package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkspec.LinkageRule

/**
 * The input of the learning algorithm.
 *
 * @param trainingEntities Reference entities used for training
 * @param validationEntities Reference entities used for validation
 * @param seedLinkageRules Existing linkage rules which are used to seed the population.
 */
case class LearningInput(trainingEntities: ReferenceEntities = ReferenceEntities.empty,
                         validationEntities: ReferenceEntities = ReferenceEntities.empty,
                         seedLinkageRules: Traversable[LinkageRule] = Traversable.empty)

object LearningInput {
  def empty = LearningInput()
}