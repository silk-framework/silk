package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.filter.RemoveValues
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = RemoveDuplicates.pluginId,
  categories = Array("Normalize"),
  label = "Remove duplicates",
  description = "Removes duplicated values, making a value sequence distinct.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveValues.pluginId,
      description = "Remove values is driven by a reference list — it drops every instance of a blacklisted word. Remove duplicates needs no such list: it keeps the first occurrence of each value and discards the rest, based solely on the input values repeating themselves."
    )
  )
)
case class RemoveDuplicates() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.distinct
  }
}

object RemoveDuplicates {
  final val pluginId = "removeDuplicates"
}
