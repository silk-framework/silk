package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.selection.RegexSelectTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

/**
 * Removes all strings that do NOT match a regex.
 */
@Plugin(
  id = FilterByRegex.pluginId,
  categories = Array("Filter"),
  label = "Filter by regex",
  description = "Removes all strings that do NOT match a regex. If 'negate' is true, only strings will be removed that match the regex.",
  relatedPlugins = Array(
    new PluginReference(
      id = RegexSelectTransformer.pluginId,
      description = "Filter by regex keeps or drops values from the input sequence based on full-string matching. Regex selection keeps the checked value out of the output and instead returns a pattern-list-shaped result filled with the provided output value where a pattern matches."
    )
  )
)
case class FilterByRegex(regex: String, negate: Boolean = false) extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if(!negate) {
      values.head.filter(str => str.matches(regex))
    } else {
      values.head.filterNot(str => str.matches(regex))
    }
  }
}

object FilterByRegex {
  final val pluginId = "filterByRegex"
}
