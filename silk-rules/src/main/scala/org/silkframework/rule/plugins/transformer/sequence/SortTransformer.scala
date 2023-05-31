package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "sort",
  categories = Array("Sequence"),
  label = "Sort",
  description = "Sorts values lexicographically."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array(),
    output = Array()
  ),
  new TransformExample(
    input1 = Array("c", "a", "b"),
    output = Array("a", "b", "c")
  ),
  new TransformExample(
    input1 = Array("Hans", "Hansa", "Hamburg"),
    output = Array("Hamburg", "Hans", "Hansa")
  )
))
case class SortTransformer() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.sorted
  }
}
