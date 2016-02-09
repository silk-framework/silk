package org.silkframework.plugins.transformer.linguistic

import com.rockymadden.stringmetric.phonetic.MetaphoneAlgorithm
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "metaphone",
  categories = Array("Linguistic"),
  label = "Metaphone",
  description = "Metaphone phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/"
)
case class MetaphoneTransformer() extends SimpleTransformer {

  override def evaluate(value: String) = {
    MetaphoneAlgorithm.compute(value).getOrElse("")
  }
}