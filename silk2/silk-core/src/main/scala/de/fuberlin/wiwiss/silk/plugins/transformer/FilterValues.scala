package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

@Plugin(id = "removeValues", label = "Remove values", description = "Removes values.")
class FilterValues(blacklist: String) extends Transformer {
  val filterValues = blacklist.split(",").map(_.trim.toLowerCase).toSet

  override def apply(values: Seq[Set[String]]) = {
    values.head.map(_.toLowerCase).filterNot(filterValues.contains)
  }
}