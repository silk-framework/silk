package org.silkframework.plugins.transformer.linguistic

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.rule.input.SimpleTransformer
import com.rockymadden.stringmetric.phonetic.MetaphoneAlgorithm

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