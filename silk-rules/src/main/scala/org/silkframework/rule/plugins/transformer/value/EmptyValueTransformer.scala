package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.filter.RemoveEmptyValues
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = EmptyValueTransformer.pluginId,
  label = "Empty value",
  categories = Array("Value"),
  description = "Generates an empty value.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveEmptyValues.pluginId,
      description = "Empty value always outputs an empty sequence, discarding all input. Remove empty values is selective: it passes non-empty strings through and drops only the empty ones."
    )
  )
)
case class EmptyValueTransformer() extends InlineTransformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq.empty
  }
}

object EmptyValueTransformer {
  final val pluginId = "emptyValue"
}
