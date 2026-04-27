package org.silkframework.rule.plugins.transformer.numeric

import java.text.NumberFormat
import java.util.Locale
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

@Plugin(
  id = PhysicalQuantityExtractor.pluginId,
  label = "Extract physical quantity",
  categories = Array("Numeric", "Normalize"),
  description = "Extracts physical quantities, such as length or weight values. Values are expected to be formatted as `{Number}{UnitPrefix}{Symbol}` and are converted to the base unit.",
  documentationFile = "PhysicalQuantityExtractor.md",
  relatedPlugins = Array(
    new PluginReference(
      id = NumOperationTransformer.pluginId,
      description = "The Physical quantity extractor plugin turns number plus unit strings into plain numeric values in the configured base unit. The Numeric operation plugin is the arithmetic reducer once the inputs are already numbers, so unit parsing and calculation stay separate."
    ),
    new PluginReference(
      id = FormatNumber.pluginId,
      description = "Extract physical quantity returns a plain numeric string in the base unit. Format number takes that value and renders it according to a decimal format pattern, controlling precision, digit grouping, and separators."
    )
  )
)
case class PhysicalQuantityExtractor(@Param("The symbol of the dimension, e.g., 'm' for meter.")
                                     symbol: String = "",
                                     @Param("The IETF BCP 47 language tag, e.g. 'en'.")
                                     numberFormat: String = "en",
                                     @Param("Only extracts from values that contain the given regex (case-insensitive).")
                                     filter: String = "",
                                     @Param("If there are multiple matches, retrieve the value with the given index (zero-based).")
                                     index: Int = 0) extends InlineTransformer {

  private val unitPrefixes = Map(
    "p" -> 0.000000000001,
    "n" -> 0.000000001,
    "μ" -> 0.000001,
    "U" -> 0.000001,
    "u" -> 0.000001,
    "m" -> 0.001,
    "c" -> 0.01,
    "d" -> 0.1,
    "da" -> 10.0,
    "h" -> 100.0,
    "k" -> 1000.0,
    "K" -> 1000.0,
    "M" -> 1000000.0,
    "G" -> 1000000000.0
  )

  private val numberParser = NumberFormat.getInstance(Locale.forLanguageTag(numberFormat))

  private val filterRegex = if(filter.nonEmpty) Some(("(?i)" + filter).r) else None

  private val dimensionRegex = {
    val number = "(-?[\\d\\.,]+)"
    val unit = "(" + unitPrefixes.keys.mkString("|") + ")?"
    val endOrNonWordCharacter = "(?:$|[^\\p{Alpha}])"

    new Regex(number + "\\s*" + unit + symbol + endOrNonWordCharacter)
  }

  override def apply(values: Seq[Seq[String]]) = {
    values.head.flatMap(evaluate)
  }

  def evaluate(value: String): Option[String] = {
    for(regex <- filterRegex if regex.findFirstIn(value).isEmpty)
      return None

    for(matches <- findMatch(value)) yield {
      val number = numberParser.parse(matches.group(1)).doubleValue()
      val factor = unitPrefixes.getOrElse(matches.group(2), 1.0)
      (number * factor).toString
    }
  }

  private def findMatch(value: String): Option[Match] = {
    if(index == 0) {
      dimensionRegex.findFirstMatchIn(value)
    } else {
      val iterator = dimensionRegex.findAllMatchIn(value).drop(index)
      if(iterator.isEmpty)
        None
      else
        Some(iterator.next())
    }
  }

}

object PhysicalQuantityExtractor {
  final val pluginId = "extractPhysicalQuantity"
}
