package de.fuberlin.wiwiss.silk.plugins.transformer.combine

import de.fuberlin.wiwiss.silk.rule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import java.util.regex.Pattern

/**
 * Transformer concatenating multiple values using a given glue string. Optionally removes duplicate values.
 * @author Florian Kleedorfer
 *
 */
@Plugin(
  id = "concatMultiValues",
  categories = Array("Combine"),
  label = "ConcatenateMultipleValues",
  description = "Concatenates multiple values received for an input. If applied to multiple inputs, yields at most one value per input. Optionally removes duplicate values."
)
case class ConcatMultipleValuesTransformer(glue: String = "", removeDuplicates:Boolean = false) extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    (for (strings <- values; if ! strings.isEmpty) yield
    {
      if (removeDuplicates) {
        //glue, split, remove duplicates and glue again to remove more subtle duplicates.
        //e.g. "Albert", "Einstein", "Albert Einstein" -> "Albert Einstein" instead of "Albert Einstein Albert Einstein"
        strings.reduce(_ + glue + _).split(Pattern.quote(glue)).toSet.reduce(_ + glue + _)
      } else {
        strings.reduce(_ + glue + _)
      }
    }).toSet
  }

}