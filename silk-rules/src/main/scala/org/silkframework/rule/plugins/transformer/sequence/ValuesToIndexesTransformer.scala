package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

/**
  * Transforms the sequence of values to their respective indexes in the sequence.
  * Example:
  * - ("a", "b", "c") becomes (0, 1, 2)
  */
@Plugin(
  id = "toSequenceIndex",
  categories = Array("Sequence"),
  label = "Sequence values to indexes",
  description =
      """
        Transforms the sequence of values to their respective indexes in the sequence.
        Example:
         - ("a", "b", "c") becomes (0, 1, 2)

        If there is more than one input, the values are numbered from the first input on and continued for the next inputs.
        Applied against an RDF source the order might not be deterministic.
      """
)
class ValuesToIndexesTransformer extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.zipWithIndex map { _._2.toString }
  }
}
