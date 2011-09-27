package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "merge", label = "Merge", description = "Merges the values of all inputs.")
class MergeTransformer extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    values.reduce(_ union _)
  }
}