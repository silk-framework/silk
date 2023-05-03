package org.silkframework.rule.plugins.transformer.linguistic

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "metaphone",
  categories = Array("Linguistic"),
  label = "Metaphone",
  description = "Metaphone phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/."
)
case class MetaphoneTransformer() extends SimpleTransformer {

  override def evaluate(value: String) = {
    //TODO MetaphoneAlgorithm.compute(value).getOrElse("")
    ???
  }
}