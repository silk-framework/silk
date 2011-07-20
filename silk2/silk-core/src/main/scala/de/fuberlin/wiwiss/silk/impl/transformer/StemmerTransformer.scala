package de.fuberlin.wiwiss.silk.impl.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "stem", label = "Stem", description = "Stems a string using the Porter Stemmer.")
class StemmerTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    val stemmer = new PorterStemmer
    stemmer.stem(value)
  }
}
