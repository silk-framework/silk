package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.Uri

@Plugin(
  id = "constantUri",
  label = "Constant URI",
  categories = Array("Value"),
  description = "Generates a constant URI."
)
case class ConstantUriTransformer(
  @Param("The constant URI to be generated")
  value: Uri = Uri("http://www.w3.org/2002/07/owl#Class")) extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(value.toString)
  }
}