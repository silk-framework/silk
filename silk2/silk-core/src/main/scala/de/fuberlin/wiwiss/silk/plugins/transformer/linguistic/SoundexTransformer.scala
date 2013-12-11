package de.fuberlin.wiwiss.silk.plugins.transformer.linguistic

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
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