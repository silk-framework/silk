package org.silkframework.rule

import org.silkframework.entity.Entity
import org.silkframework.util.Identifier

/**
  * The index values of an entity with regards to a [[LinkageRule]].
  * In comparison to the [[org.silkframework.entity.Index]] object, this data structure links the index values to
  * the comparator inputs. Also this index uses the full Int range as value domain instead reducing it to the number of blocks.
  */
case class LinkageRuleIndex(root: LinkageRuleIndexSimilarityOperator)

sealed trait LinkageRuleIndexNode {
  // This id is the same as of the corresponding linkage rule operator
  def id: Identifier
}

sealed trait LinkageRuleIndexSimilarityOperator extends LinkageRuleIndexNode

case class LinkageRuleIndexAggregator(id: Identifier, children: LinkageRuleIndexSimilarityOperator) extends LinkageRuleIndexSimilarityOperator

case class LinkageRuleIndexComparison(id: Identifier,
                                      source: LinkageRuleIndexInput,
                                      target: LinkageRuleIndexInput) extends LinkageRuleIndexSimilarityOperator

// The actual index values of a specific input of a comparison
case class LinkageRuleIndexInput(id: Identifier, indexValues: Set[Int])

object LinkageRuleIndex {
  def apply(entity: Entity,
            sourceOrTarget: Boolean,
           // TODO: Remove limit?
            limit: Double = 0.0): LinkageRuleIndex = {
    null // TODO
  }
}