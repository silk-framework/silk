package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "concat", label = "Concatenate", description = "Concatenates strings from two inputs.")
class ConcatTransformer(glue: String = "") extends Transformer {
  override def apply(values: Seq[Traversable[String]]): Traversable[String] = {
    for (sequence <- cartesianProduct(values)) yield evaluate(sequence)
  }

  private def cartesianProduct(strings: Seq[Traversable[String]]): Traversable[List[String]] = {
    if (strings.tail.isEmpty) for (string <- strings.head) yield string :: Nil
    else for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
  }

  private def evaluate(strings: Seq[String]) = {
    (strings.head /: strings.tail)(_ + glue + _)
  }
}