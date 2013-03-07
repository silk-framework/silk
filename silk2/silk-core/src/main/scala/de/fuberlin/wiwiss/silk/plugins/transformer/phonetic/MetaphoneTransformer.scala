package de.fuberlin.wiwiss.silk.plugins.transformer.phonetic

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import com.rockymadden.stringmetric.phonetic.MetaphoneAlgorithm

@Plugin(id = "metaphone", label = "Metaphone", description = "Metaphone phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/")
case class MetaphoneTransformer() extends SimpleTransformer {

  private val metaphone =  new MetaphoneAlgorithm()

  override def evaluate(value: String) = {
    metaphone.compute(value).getOrElse("")
  }
}