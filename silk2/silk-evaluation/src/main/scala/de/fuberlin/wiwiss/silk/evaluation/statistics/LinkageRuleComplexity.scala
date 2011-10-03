package de.fuberlin.wiwiss.silk.evaluation.statistics


import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}

/**
 * Complexity measures of a linkage rule.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class LinkageRuleComplexity(comparisonCount: Int, transformationCount: Int)

/**
 * Evaluates the complexity of a linkage rule.
 */
object LinkageRuleComplexity {
  /**
   * Evaluates the complexity of a linkage rule.
   */
  def apply(linkageRule: LinkageRule): LinkageRuleComplexity = {
    LinkageRuleComplexity(
      comparisonCount = collectOperators(linkageRule).filter(_.isInstanceOf[Comparison]).size,
      transformationCount = collectOperators(linkageRule).filter(_.isInstanceOf[TransformInput]).size
    )
  }

  /**
   * Collects all operators of the linkage rule.
   */
  private def collectOperators(linkageRule: LinkageRule): Traversable[Operator] = {
    linkageRule.operator.toTraversable.flatMap(collectOperators)
  }

  /**
   * Collects all operators of the linkage rule.
   */
  private def collectOperators(root: Operator): Traversable[Operator] = root match {
    case Aggregation(_, _, _, _, ops) => root +: ops.flatMap(collectOperators)
    case Comparison(_, _, _, _, _, inputs) => root +: inputs.flatMap(collectOperators)
    case TransformInput(_, _, inputs) => root +: inputs.flatMap(collectOperators)
    case PathInput(_, _) => Traversable(root)
  }
}
