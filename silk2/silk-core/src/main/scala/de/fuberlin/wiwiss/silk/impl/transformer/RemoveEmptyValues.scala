package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

@StrategyAnnotation(id = "removeEmptyValues", label = "Remove empty values", description = "Removes empty values.")
class RemoveEmptyValues() extends Transformer {
  override def apply(values: Seq[Set[String]]) = {
    values.head.filter(!_.isEmpty)
  }
}