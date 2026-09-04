package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

/**
 * Removes all strings that are shorter than 'min' characters and longer than 'max' characters.
 */
@Plugin(
  id = FilterByLength.pluginId,
  categories = Array("Filter"),
  label = "Filter by length",
  description = "Removes all strings that are shorter than 'min' characters and longer than 'max' characters.",
  relatedPlugins = Array(
    new PluginReference(
      id = FilterByRegex.pluginId,
      description = "Filter by regex tests each value against a pattern; the length is never a factor. Filter " +
        "by length checks the length bounds only; a pattern is never a factor."
    )
  )
)
case class FilterByLength(min: Int = 0, max: Int = Int.MaxValue) extends InlineTransformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] =
    values.head.filterNot(str => str.length < min || str.length > max)
}

object FilterByLength {
  final val pluginId = "filterByLength"
}
