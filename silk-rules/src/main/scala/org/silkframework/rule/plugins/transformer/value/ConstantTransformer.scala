package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{Param, Plugin, PluginCategories}

@Plugin(
  id = "constant",
  label = "Constant",
  categories = Array("Value", PluginCategories.recommended),
  description = "Generates a constant value."
)
case class ConstantTransformer(
  @Param("The constant value to be generated")
  value: String = "") extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(value)
  }
}