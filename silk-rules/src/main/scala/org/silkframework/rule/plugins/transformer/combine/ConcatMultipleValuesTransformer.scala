package org.silkframework.rule.plugins.transformer.combine

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import java.util.regex.Pattern

import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

/**
 * Transformer concatenating multiple values using a given glue string. Optionally removes duplicate values.
 * @author Florian Kleedorfer
 *
 */
@Plugin(
  id = ConcatMultipleValuesTransformer.pluginId,
  categories = Array("Combine"),
  label = "Concatenate multiple values",
  description = "Concatenates multiple values received for an input. If applied to multiple inputs, yields at most one value per input. Optionally removes duplicate values.",
  relatedPlugins = Array(
    new PluginReference(
      id = ConcatTransformer.pluginId,
      description = "Concatenate multiple values collapses all values within each input into one string, preserving the boundary between inputs. Concatenate crosses that boundary — it takes one value from each input and produces all combinations, so the output grows with the number of inputs and values."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "Without input values, no output is generated.",
    output = Array()
  ),
  new TransformExample(
    description = "A single value is returned unchanged.",
    input1 = Array("a"),
    output = Array("a")
  ),
  new TransformExample(
    description = "All values of an input are concatenated into one value. The default glue is the empty string.",
    input1 = Array("a", "b"),
    output = Array("ab")
  ),
  new TransformExample(
    description = "The glue string is inserted between the values.",
    parameters = Array("glue", "x"),
    input1 = Array("a", "b"),
    output = Array("axb")
  ),
  new TransformExample(
    description = "Each input is concatenated separately, yielding one value per input.",
    input1 = Array("a", "b"),
    input2 = Array("1", "2"),
    output = Array("ab", "12")
  ),
  new TransformExample(
    description = "Escaped character sequences in the glue are replaced by the actual characters (newline, tab, backslash).",
    parameters = Array("glue", "\\n\\t\\\\"),
    input1 = Array("a\n\t\\b", "c"),
    output = Array("a\n\t\\b\n\t\\c")
  ),
  new TransformExample(
    description = "Duplicates are removed, also when they span multiple values.",
    parameters = Array("glue", " ", "removeDuplicates", "true"),
    input1 = Array("Albert", "Einstein", "Albert Einstein"),
    output = Array("Albert Einstein")
  ),
  new TransformExample(
    description = "With an empty glue, only whole duplicate values are removed.",
    parameters = Array("removeDuplicates", "true"),
    input1 = Array("a", "b", "a"),
    output = Array("ab")
  ),
  new TransformExample(
    description = "Values consisting only of the glue collapse to an empty string.",
    parameters = Array("glue", "x", "removeDuplicates", "true"),
    input1 = Array("x", "x"),
    output = Array("")
  )
))
case class ConcatMultipleValuesTransformer(glue: String = "", removeDuplicates: Boolean = false) extends InlineTransformer {
  // glue with escaped char sequences (\\, \n, \t) converted to actual character.
  lazy val parsedGlue: String = ConcatTransformer.parseGlue(glue)

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    for (strings <- values; if strings.nonEmpty) yield {
      if (removeDuplicates && parsedGlue.nonEmpty) {
        //glue, split, remove duplicates and glue again to remove more subtle duplicates.
        //e.g. "Albert", "Einstein", "Albert Einstein" -> "Albert Einstein" instead of "Albert Einstein Albert Einstein"
        strings.mkString(parsedGlue).split(Pattern.quote(parsedGlue)).distinct.mkString(parsedGlue)
      } else if (removeDuplicates) {
        //an empty glue offers no split boundary, so only whole values are deduplicated.
        strings.distinct.mkString
      } else {
        strings.reduce(_ + parsedGlue + _)
      }
    }
  }
}

object ConcatMultipleValuesTransformer {
  final val pluginId = "concatMultiValues"
}
