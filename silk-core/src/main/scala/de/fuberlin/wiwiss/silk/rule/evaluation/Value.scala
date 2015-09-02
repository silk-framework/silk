package de.fuberlin.wiwiss.silk.rule.evaluation

import de.fuberlin.wiwiss.silk.rule.input.{Input, PathInput, TransformInput}

/**
 * An intermediate value of a input operator evaluation.
 */
sealed trait Value {
  /**
   * The corresponding input that generated this value
   */
  def input: Input

  /**
   * The values the resulted from evaluating the corresponding input.
   */
  def values: Set[String]

  /**
   * The intermediate values of the children operators of the corresponding input.
   */
  def children: Seq[Value]
}

/**
 * An intermediate value of a transformation evaluation.
 */
case class TransformedValue(input: TransformInput, values: Set[String], children: Seq[Value]) extends Value

/**
 * An intermediate value of a path input evaluation.
 */
case class InputValue(input: PathInput, values: Set[String]) extends Value {
  def children = Seq.empty
}