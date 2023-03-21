package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.PluginCategories

@Plugin(
  id = "constant",
  label = "Constant",
  categories = Array("Value", PluginCategories.recommended),
  description = "Generates a constant value."
)
@TransformExamples(Array(
  new TransformExample(
    description = "Always outputs the specified value.",
    parameters = Array("value", "John"),
    output = Array("John")
  ),
))
case class ConstantTransformer(
  @Param("The constant value to be generated")
  value: String = "") extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(value)
  }
}