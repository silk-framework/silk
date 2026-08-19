package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

/**
  * For each input sequence, take the element at the specified index — a negative index counts from the end —
  * and collect the results into a single sequence.
  * If one input has no element at that index, it is either ignored or the transformation fails.
  */
@Plugin(
  id = "getValueByIndex",
  categories = Array("Sequence", "Tokenization"),
  label = "Get value by index",
  description =
    """Returns the value found at the specified index. A negative index counts from the end of the sequence, e.g. -1 returns the last value.
       Fails or returns an empty result depending on failIfNotFound is set or not.
       Please be aware that this will work only if the data source supports some kind of ordering like XML or JSON. This
       is probably not a good idea to do with RDF models.

       If emptyStringToEmptyResult is true then instead of a result with an empty String, an empty result is returned.
    """
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
  @Param("The index of the value to return. A non-negative index counts from the start (0-based). A negative index counts from the end, so -1 is the last value, -2 is the second-to-last, and so on.")
  index: Int,
  failIfNotFound: Boolean = false,
  emptyStringToEmptyResult: Boolean = false
) extends InlineTransformer {

  private implicit class IntOps(private val a: Int) {
    /**
      * The mathematical modulo operation: the result in [0, n) for positive 'n', or (n, 0] for negative 'n'.
      * Always congruent to 'a'.
      */
    def mod(n: Int): Int = ((a % n) + n) % n
  }

  private implicit class SeqOps[A](private val vs: Seq[A]) {
    def getAt(idx: Int): Option[A] = {
      val length = vs.length
      if (idx >= -length && idx < length) Some(vs(idx mod length)) else None
    }
  }

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
