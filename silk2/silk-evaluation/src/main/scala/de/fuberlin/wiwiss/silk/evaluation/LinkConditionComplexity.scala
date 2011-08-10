package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.linkspec.{Operator, LinkCondition}
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkspec.input.{PathInput, TransformInput}

/**
 * Complexity measures of a link specification.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class LinkConditionComplexity(comparisonCount: Int, transformationCount: Int)

/**
 * Evaluates the complexity of a link condition.
 */
object LinkConditionComplexity {
  /**
   * Evaluates the complexity of a link condition.
   */
  def apply(condition: LinkCondition): LinkConditionComplexity = {

    println(collectOperators(condition).toList)

    LinkConditionComplexity(
      comparisonCount = collectOperators(condition).filter(_.isInstanceOf[Comparison]).size,
      transformationCount = collectOperators(condition).filter(_.isInstanceOf[TransformInput]).size
    )
  }

  /**
   * Collects all operators of the link condition.
   */
  private def collectOperators(condition: LinkCondition): Traversable[Operator] = {
    condition.rootOperator.toTraversable.view.flatMap(collectOperators)
  }

  /**
   * Collects all operators of the link condition.
   */
  private def collectOperators(root: Operator): Traversable[Operator] = root match {
    case Aggregation(_, _, _, ops, _) => ops ++ ops.flatMap(collectOperators)
    case Comparison(_, _, _, _, inputs, _) => inputs ++ inputs.flatMap(collectOperators)
    case TransformInput(_, inputs, _) => inputs ++ inputs.flatMap(collectOperators)
    case PathInput(_, _) => Traversable(root)
  }
}
