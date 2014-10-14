package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "constant",
  label = "Constant",
  categories = Array("Recommended"),
  description = "Generates a constant value."
)
case class ConstantTransformer(value: String = "") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    Set(value)
  }
}