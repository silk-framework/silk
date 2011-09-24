package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

@StrategyAnnotation(id = "tokenize", label = "Tokenize", description = "Tokenizes all input values.")
class Tokenizer(regex: String = "\\s") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    values.head.flatMap(_.split(regex))
  }
}