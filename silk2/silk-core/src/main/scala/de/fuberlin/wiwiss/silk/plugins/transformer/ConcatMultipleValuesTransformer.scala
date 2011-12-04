package de.fuberlin.wiwiss.silk.plugins.transformer

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "concatMultiValues", label = "ConcatenateMultipleValues", description = "Concatenates multiple values " +
  "received " +
  "for an input. If applied to multiple inputs, yields at most one value per input.")
case class ConcatMultipleValuesTransformer(glue: String = "") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    (for (strings <- values; if ! strings.isEmpty) yield
    {
      strings.reduce(_ + glue + _)
    }).toSet
  }

}