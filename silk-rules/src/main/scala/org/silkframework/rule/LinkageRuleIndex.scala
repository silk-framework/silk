package org.silkframework.rule

import org.silkframework.entity.Entity
import org.silkframework.util.Identifier

/**
  * The index values of an entity with regards to a [[LinkageRule]].
  * In comparison to the [[org.silkframework.entity.Index]] object, this data structure links the index values to
  * the comparator inputs. Also this index uses the full Int range as value domain instead reducing it to the number of blocks.
  */
case class LinkageRuleIndex(root: LinkageRuleIndexSimilarityOperator) {
  def comparisons: Seq[LinkageRuleIndexComparison] = {
    LinkageRuleIndex.comparisons(root)
  }

  def comparisonIndexValues(comparisonIds: Seq[String]): Seq[Set[Int]] = {
    val cs = comparisons
    comparisonIds.map { comparisonId =>
      cs.find(_.id.toString == comparisonId).map(_.indexValues.indexValues).getOrElse(Set.empty)
    }
  }
}

sealed trait LinkageRuleIndexNode

sealed trait LinkageRuleIndexSimilarityOperator extends LinkageRuleIndexNode

case class LinkageRuleIndexAnd(children: Seq[LinkageRuleIndexSimilarityOperator]) extends LinkageRuleIndexSimilarityOperator

case class LinkageRuleIndexOr(children: Seq[LinkageRuleIndexSimilarityOperator]) extends LinkageRuleIndexSimilarityOperator

case class LinkageRuleIndexNot(child: LinkageRuleIndexSimilarityOperator) extends LinkageRuleIndexSimilarityOperator

case class LinkageRuleIndexComparison(id: Identifier,
                                      indexValues: LinkageRuleIndexInput) extends LinkageRuleIndexSimilarityOperator

// The actual index values of a specific input of a comparison
case class LinkageRuleIndexInput(id: Identifier, indexValues: Set[Int])

object LinkageRuleIndex {
  def apply(entity: Entity,
            booleanLinkageRule: BooleanLinkageRule,
            sourceOrTarget: Boolean): LinkageRuleIndex = {
    LinkageRuleIndex(convert(booleanLinkageRule.root, entity, sourceOrTarget))
  }

  private def convert(booleanOperator: BooleanOperator,
                      entity: Entity,
                      sourceOrTarget: Boolean): LinkageRuleIndexSimilarityOperator = {
    booleanOperator match {
      case BooleanAnd(children) => LinkageRuleIndexAnd(children.map(convert(_, entity, sourceOrTarget)))
      case BooleanOr(children) => LinkageRuleIndexOr(children.map(convert(_, entity, sourceOrTarget)))
      case BooleanNot(child) => LinkageRuleIndexNot(convert(child, entity, sourceOrTarget))
      case BooleanComparisonOperator(id, sourceInput, targetInput, comparison) =>
        val inputId = if(sourceOrTarget) sourceInput.inputOperator.id else targetInput.inputOperator.id
        val index = comparison.index(entity, sourceOrTarget, limit = 0.0)
        LinkageRuleIndexComparison(id, LinkageRuleIndexInput(inputId, index.flatten))
    }
  }

  def apply(entity: Entity,
            linkageRule: LinkageRule,
            sourceOrTarget: Boolean): LinkageRuleIndex = {
    BooleanLinkageRule(linkageRule) match {
      case Some(booleanLinkSpec) =>
        apply(entity, booleanLinkSpec, sourceOrTarget)
      case None =>
        throw new IllegalArgumentException("Link specification is not a boolean link specification!")
    }
  }

  /** Returns all comparison nodes of the linkage rule index */
  def comparisons(operator: LinkageRuleIndexSimilarityOperator): Seq[LinkageRuleIndexComparison] = {
    operator match {
      case LinkageRuleIndexAnd(children) =>
        children.flatMap(comparisons)
      case LinkageRuleIndexOr(children) =>
        children.flatMap(comparisons)
      case LinkageRuleIndexNot(child) =>
        comparisons(child)
      case comparison: LinkageRuleIndexComparison =>
        Seq(comparison)
    }
  }
}