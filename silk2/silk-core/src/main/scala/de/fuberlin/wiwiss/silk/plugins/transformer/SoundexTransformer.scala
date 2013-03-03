package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import com.rockymadden.stringmetric.phonetic.SoundexAlgorithm

@Plugin(id = "soundex", label = "Soundex", description = "Soundex algorithm.")
case class SoundexTransformer() extends SimpleTransformer {

  private val soundex = new SoundexAlgorithm()

  override def evaluate(value: String) = {
    soundex.compute(value).getOrElse("")
  }
}