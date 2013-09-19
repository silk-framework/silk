package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer

/**
 * Removes all strings that do NOT match a regex.
 */
@Plugin(
  id = "filterByRegex",
  label = "filterByRegex",
  description = "Removes all strings that do NOT match a regex. If 'negate' is true, only strings will be retained that match the regex."
)
case class FilterByRegex(regex: String, negate: Boolean = false) extends Transformer {

  override def apply(values: Seq[Set[String]]) = {
    if(!negate)
      values.head.filterNot(str => str.matches(regex))
    else
      values.head.filter(str => str.matches(regex))
  }
}
