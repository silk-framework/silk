package org.silkframework.rule.plugins.transformer.selection

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@TransformExamples(Array(
  new TransformExample(
    input1 = Array(),
    input2 = Array(""),
    input3 = Array(),
    output = Array("")
  ),
  new TransformExample(
    input1 = Array(),
    input2 = Array(),
    output = Array()
  ),
  new TransformExample(
    output = Array()
  ),
  new TransformExample(
    input1 = Array(),
    input2 = Array("first"),
    input3 = Array("second"),
    output = Array("first")
  ),
  new TransformExample(
    input1 = Array(),
    input2 = Array("first A", "first B"),
    input3 = Array("second"),
    output = Array("first A", "first B")
  ),
  new TransformExample(
    input1 = Array("first"),
    input2 = Array("second"),
    output = Array("first")
  ),
))
@Plugin(
  id = "coalesce",
  label = "Coalesce (first non-empty input)",
  categories = Array("Selection"),
  description = "Forwards the first non-empty input, i.e. for which any value(s) exist. A single empty string is considered a value."
)
case class CoalesceTransformer() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.find(_.nonEmpty).getOrElse(Seq.empty)
  }
}
