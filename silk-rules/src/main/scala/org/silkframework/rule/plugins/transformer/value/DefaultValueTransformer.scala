package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "defaultValue",
  label = "Default Value",
  categories = Array("Value", PluginCategories.recommended),
  description = "Generates a default value, if the input values are empty. Forwards any non-empty values."
)
@TransformExamples(Array(
  new TransformExample(
    description = "Forwards input values.",
    input1 = Array("input value"),
    output = Array("input value")
  ),
  new TransformExample(
    description = "Outputs the default value, if the inputs are empty.",
    parameters = Array("value" , "default value"),
    input1 = Array(),
    output = Array("default value")
  )
))
case class DefaultValueTransformer(
                                @Param("The default value to be generated, if input values are empty")
                                value: String = "default") extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    val allValues = values.flatten
    if(allValues.isEmpty) {
      Seq(value)
    } else {
      allValues
    }
  }
}
