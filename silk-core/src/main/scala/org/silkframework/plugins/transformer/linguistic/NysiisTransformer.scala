package org.silkframework.plugins.transformer.linguistic

import com.rockymadden.stringmetric.phonetic.{NysiisAlgorithm, RefinedNysiisAlgorithm}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "NYSIIS",
  categories = Array("Linguistic"),
  label = "NYSIIS",
  description = "NYSIIS phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/"
)
case class NysiisTransformer(refined: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String) = {
    if(refined)
      RefinedNysiisAlgorithm.compute(value).getOrElse("")
    else
      NysiisAlgorithm.compute(value).getOrElse("")
  }
}
