package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.indexable.instances.seq
import org.silkframework.util.indexable.syntax._

/**
  * For each input sequence, take the element at the specified index — a negative index counts from the end —
  * and collect the results into a single sequence.
  * If one input has no element at that index, it is either ignored or the transformation fails.
  */
@Plugin(
  id = "getValueByIndex",
  categories = Array("Sequence", "Tokenization"),
  label = "Get value by index",
  description = "Returns the value found at the specified index. A negative index counts from the end of the sequence.",
  documentationFile = "GetValueByIndexTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    description = "A negative index counts from the end; -1 returns the last value.",
    parameters = Array("index", "-1"),
    input1 = Array("a", "b", "c"),
    output = Array("c")
  ),
  new TransformExample(
    description = "An index equal to the negative sequence length wraps to the first value.",
    parameters = Array("index", "-3"),
    input1 = Array("a", "b", "c"),
    output = Array("a")
  ),
  new TransformExample(
    description = "A negative index past the start of the sequence returns an empty result by default.",
    parameters = Array("index", "-2"),
    input1 = Array("a"),
    output = Array()
  ),
  new TransformExample(
    description = "A negative index past the start of the sequence throws if 'failIfNotFound' is enabled.",
    parameters = Array("index", "-2", "failIfNotFound", "true"),
    input1 = Array("a"),
    throwsException = classOf[IndexOutOfBoundsException]
  )
))
case class GetValueByIndexTransformer(
  @Param("The index of the value: 0-based if non-negative; if negative, counts from the end, wrapping at -length.")
  index: Int,
  @Param("If enabled, the transformation fails when no value exists at the given index; otherwise it is dropped.")
  failIfNotFound: Boolean = false,
  @Param("If enabled, an empty-string result is treated as no result, instead of being returned as an empty string.")
  emptyStringToEmptyResult: Boolean = false
) extends InlineTransformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatMap { vs =>
      vs.getAt(index) match {
        case None if failIfNotFound =>
          throw new IndexOutOfBoundsException("No value at index " + index + ".")
        case None =>
          Seq()
        case Some(v) if emptyStringToEmptyResult && v.isEmpty =>
          Seq()
        case Some(v) =>
          Seq(v)
      }
    }
  }
}
