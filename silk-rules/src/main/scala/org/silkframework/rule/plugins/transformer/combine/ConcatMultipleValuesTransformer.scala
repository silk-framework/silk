package org.silkframework.rule.plugins.transformer.combine

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import java.util.regex.Pattern

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

/**
 * Transformer concatenating multiple values using a given glue string. Optionally removes duplicate values.
 * @author Florian Kleedorfer
 *
 */
@Plugin(
  id = "concatMultiValues",
  categories = Array("Combine"),
  label = "Concatenate multiple values",
  description = "Concatenates multiple values received for an input. If applied to multiple inputs, yields at most one value per input. Optionally removes duplicate values."
)
@TransformExamples(Array(
  new TransformExample(
    output = Array()
  ),
  new TransformExample(
    input1 = Array("a"),
    output = Array("a")
  ),
  new TransformExample(
    input1 = Array("a", "b"),
    output = Array("ab")
  ),
  new TransformExample(
    parameters = Array("glue", "x"),
    input1 = Array("a", "b"),
    output = Array("axb")
  ),
  new TransformExample(
    input1 = Array("a", "b"),
    input2 = Array("1", "2"),
    output = Array("ab", "12")
  ),
  new TransformExample(
    parameters = Array("glue", "\\n\\t\\\\"),
    input1 = Array("a\n\t\\b", "c"),
    output = Array("a\n\t\\b\n\t\\c")
  )
))
case class ConcatMultipleValuesTransformer(glue: String = "", removeDuplicates:Boolean = false) extends Transformer {
  // glue with escaped char sequences (\\, \n, \t) converted to actual character.
  lazy val parsedGlue: String = ConcatTransformer.parseGlue(glue)

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    for (strings <- values; if strings.nonEmpty) yield {
      if (removeDuplicates) {
        //glue, split, remove duplicates and glue again to remove more subtle duplicates.
        //e.g. "Albert", "Einstein", "Albert Einstein" -> "Albert Einstein" instead of "Albert Einstein Albert Einstein"
        strings.reduce(_ + parsedGlue + _).split(Pattern.quote(parsedGlue)).reduce(_ + parsedGlue + _)
      } else {
        strings.reduce(_ + parsedGlue + _)
      }
    }
  }
}
