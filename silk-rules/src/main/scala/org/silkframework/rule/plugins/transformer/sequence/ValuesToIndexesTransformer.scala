package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

/**
  * Transforms the sequence of values to their respective indexes in the sequence.
  * Example:
  * - ("a", "b", "c") becomes (0, 1, 2)
  */
@Plugin(
  id = "toSequenceIndex",
  categories = Array("Sequence"),
  label = "Sequence values to indexes",
  description = """Transforms the sequence of values to their respective indexes in the sequence. If there is more than one input, the values are numbered from the first input on and continued for the next inputs. Applied against an RDF source the order might not be deterministic."""
)
@TransformExamples(
  Array(new TransformExample(
    description = "Transforms the sequence of values to their respective indexes in the sequence.",
    input1 = Array("a", "b", "c"),
    output = Array("0", "1", "2")
  ))
)
case class ValuesToIndexesTransformer() extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.zipWithIndex map { _._2.toString }
  }
}
