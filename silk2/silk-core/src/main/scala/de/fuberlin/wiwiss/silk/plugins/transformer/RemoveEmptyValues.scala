package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkspec.input.Transformer

@Plugin(id = "removeEmptyValues", label = "Remove empty values", description = "Removes empty values.")
class RemoveEmptyValues() extends Transformer {
  override def apply(values: Seq[Set[String]]) = {
    values.head.filter(!_.isEmpty)
  }
}