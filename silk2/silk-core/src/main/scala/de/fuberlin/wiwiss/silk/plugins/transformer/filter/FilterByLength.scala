package de.fuberlin.wiwiss.silk.plugins.transformer.filter

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.{Transformer, SimpleTransformer}

/**
 * Removes all strings that are shorter than 'min' characters and longer than 'max' characters.
 */
@Plugin(
  id = "filterByLength",
  categories = Array("filter"),
  label = "filter by length",
  description = "Removes all strings that are shorter than 'min' characters and longer than 'max' characters."
)
case class FilterByLength(min: Int = 0, max: Int = Int.MaxValue) extends Transformer {

  override def apply(values: Seq[Set[String]]) = {
    values.head.filterNot(str => str.length < min || str.length > max)
  }
}
