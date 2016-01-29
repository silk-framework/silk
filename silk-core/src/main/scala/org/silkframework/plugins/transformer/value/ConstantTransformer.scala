package org.silkframework.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{Param, Plugin}

@Plugin(
  id = "constant",
  label = "Constant",
  categories = Array("Recommended", "Value"),
  description = "Generates a constant value."
)
case class ConstantTransformer(
  @Param("The constant value to be generated")
  value: String = "") extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(value)
  }
}