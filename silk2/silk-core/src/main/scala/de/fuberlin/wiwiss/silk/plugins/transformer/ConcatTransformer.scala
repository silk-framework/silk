package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "concat", label = "Concatenate", description = "Concatenates strings from two inputs.")
class ConcatTransformer(glue: String = "") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    for (sequence <- cartesianProduct(values)) yield evaluate(sequence)
  }

  private def cartesianProduct(strings: Seq[Set[String]]): Set[List[String]] = {
    if (strings.tail.isEmpty) for (string <- strings.head) yield string :: Nil
    else for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
  }

  private def evaluate(strings: Seq[String]) = {
    (strings.head /: strings.tail)(_ + glue + _)
  }
}