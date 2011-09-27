package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkspec.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "stem", label = "Stem", description = "Stems a string using the Porter Stemmer.")
class StemmerTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    val stemmer = new PorterStemmer
    stemmer.stem(value)
  }
}
