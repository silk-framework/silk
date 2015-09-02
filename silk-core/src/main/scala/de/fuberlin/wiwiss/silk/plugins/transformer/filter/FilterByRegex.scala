package de.fuberlin.wiwiss.silk.plugins.transformer.filter

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.rule.input.Transformer

/**
 * Removes all strings that do NOT match a regex.
 */
@Plugin(
  id = "filterByRegex",
  categories = Array("Filter"),
  label = "filter by regex",
  description = "Removes all strings that do NOT match a regex. If 'negate' is true, only strings will be removed that match the regex."
)
case class FilterByRegex(regex: String, negate: Boolean = false) extends Transformer {

  override def apply(values: Seq[Set[String]]) = {
    if(!negate)
      values.head.filter(str => str.matches(regex))
    else
      values.head.filterNot(str => str.matches(regex))
  }
}
