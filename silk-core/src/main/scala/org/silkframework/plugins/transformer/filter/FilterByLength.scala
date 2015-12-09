package org.silkframework.plugins.transformer.filter

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.rule.input.{Transformer, SimpleTransformer}

/**
 * Removes all strings that are shorter than 'min' characters and longer than 'max' characters.
 */
@Plugin(
  id = "filterByLength",
  categories = Array("Filter"),
  label = "filter by length",
  description = "Removes all strings that are shorter than 'min' characters and longer than 'max' characters."
)
case class FilterByLength(min: Int = 0, max: Int = Int.MaxValue) extends Transformer {

  override def apply(values: Seq[Seq[String]]) = {
    values.head.filterNot(str => str.length < min || str.length > max)
  }
}
