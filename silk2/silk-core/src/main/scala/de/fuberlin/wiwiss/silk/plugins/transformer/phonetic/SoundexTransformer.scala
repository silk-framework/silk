package de.fuberlin.wiwiss.silk.plugins.transformer.phonetic

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import com.rockymadden.stringmetric.phonetic.{RefinedSoundexAlgorithm, SoundexAlgorithm}

@Plugin(id = "soundex", label = "Soundex", description = "Soundex algorithm. Provided by the StringMetric library: http://rockymadden.com/stringmetric/")
case class SoundexTransformer(refined: Boolean = true) extends SimpleTransformer {

  private val soundex = if(refined) new RefinedSoundexAlgorithm() else new SoundexAlgorithm()

  override def evaluate(value: String) = {
    soundex.compute(value).getOrElse("")
  }
}