package org.silkframework.plugins.transformer.linguistic

import com.rockymadden.stringmetric.phonetic.{RefinedSoundexAlgorithm, SoundexAlgorithm}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "soundex",
  categories = Array("Linguistic"),
  label = "Soundex",
  description = "Soundex algorithm. Provided by the StringMetric library: http://rockymadden.com/stringmetric/"
)
case class SoundexTransformer(refined: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String) = {
    if(refined)
      RefinedSoundexAlgorithm.compute(value).getOrElse("")
    else
      SoundexAlgorithm.compute(value).getOrElse("")
  }
}