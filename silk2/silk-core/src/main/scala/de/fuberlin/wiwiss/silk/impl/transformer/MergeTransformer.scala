package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.Transformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "merge", label = "Merge", description = "Merges the values of all inputs.")
class MergeTransformer extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    values.reduce(_ union _)
  }
}