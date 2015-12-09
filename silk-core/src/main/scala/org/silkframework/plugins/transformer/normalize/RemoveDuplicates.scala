package org.silkframework.plugins.transformer.normalize

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "removeDuplicates",
  categories = Array("Normalize"),
  label = "Remove duplicates",
  description = "Removes duplicated values, making a value sequence distinct."
)
case class RemoveDuplicates() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.distinct
  }
}
