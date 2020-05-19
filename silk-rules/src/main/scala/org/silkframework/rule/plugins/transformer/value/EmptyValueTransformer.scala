package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "emptyValue",
  label = "Empty value",
  categories = Array("Value"),
  description = "Generates an empty value value."
)
case class EmptyValueTransformer() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq.empty
  }
}
