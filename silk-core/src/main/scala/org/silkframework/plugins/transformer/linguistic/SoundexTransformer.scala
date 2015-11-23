package org.silkframework.plugins.transformer.linguistic

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.rule.input.SimpleTransformer
import com.rockymadden.stringmetric.phonetic.{RefinedSoundexAlgorithm, SoundexAlgorithm}

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