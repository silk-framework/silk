package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkspec.input.{PathInput, TransformInput}

/**
 * Complexity measures of a link specification.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class LinkageRuleComplexity(comparisonCount: Int, transformationCount: Int)

/**
 * Evaluates the complexity of a link condition.
 */
object LinkageRuleComplexity {
  /**
   * Evaluates the complexity of a link condition.
   */
  def apply(linkageRule: LinkageRule): LinkageRuleComplexity = {
    LinkageRuleComplexity(
      comparisonCount = collectOperators(linkageRule).filter(_.isInstanceOf[Comparison]).size,
      transformationCount = collectOperators(linkageRule).filter(_.isInstanceOf[TransformInput]).size
    )
  }

  /**
   * Collects all operators of the link condition.
   */
  private def collectOperators(linkageRule: LinkageRule): Traversable[Operator] = {
    linkageRule.operator.toTraversable.flatMap(collectOperators)
  }

  /**
   * Collects all operators of the link condition.
   */
  private def collectOperators(root: Operator): Traversable[Operator] = root match {
    case Aggregation(_, _, _, ops, _) => root +: ops.flatMap(collectOperators)
    case Comparison(_, _, _, _, inputs, _) => root +: inputs.flatMap(collectOperators)
    case TransformInput(_, inputs, _) => root +: inputs.flatMap(collectOperators)
    case PathInput(_, _) => Traversable(root)
  }
}
