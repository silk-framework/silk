package de.fuberlin.wiwiss.silk.linkspec.input

/**
 * Simple transformer which transforms all values of the first input.
 */
abstract class SimpleTransformer extends Transformer {
  override final def apply(values: Seq[Set[String]]): Set[String] = {
    values.head.map(evaluate)
  }

  def evaluate(value: String): String
}
