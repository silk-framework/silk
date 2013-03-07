package de.fuberlin.wiwiss.silk.plugins.transformer.phonetic

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import com.rockymadden.stringmetric.phonetic.{RefinedNysiisAlgorithm, NysiisAlgorithm}
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

@Plugin(id = "NYSIIS", label = "NYSIIS", description = "NYSIIS phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/")
case class NysiisTransformer(refined: Boolean = true) extends SimpleTransformer {

  private val nysiis = if(refined) new RefinedNysiisAlgorithm() else new NysiisAlgorithm()

  override def evaluate(value: String) = {
    nysiis.compute(value).getOrElse("")
  }
}
