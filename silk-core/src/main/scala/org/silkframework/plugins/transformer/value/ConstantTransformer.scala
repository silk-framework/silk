package org.silkframework.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "constant",
  label = "Constant",
  categories = Array("Recommended", "Value"),
  description = "Generates a constant value."
)
case class ConstantTransformer(value: String = "") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    Set(value)
  }
}