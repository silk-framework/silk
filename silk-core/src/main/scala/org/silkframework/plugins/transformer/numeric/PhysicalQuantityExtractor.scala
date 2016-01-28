package org.silkframework.plugins.transformer.numeric

import java.text.NumberFormat
import java.util.Locale

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "extractPhysicalQuantity",
  label = "Physical Quantity Extractor",
  categories = Array("Numeric", "Normalize"),
  description =
"""Extracts physical quantities, such as length or weight values.
Values are expected of the form '{Number}{UnitPrefix}{Symbol}' and are converted to the base unit.

Example:

- Given a value '10km, 3mg'.
- If the symbol parameter is set to 'm', the extracted value is 10000.
- If the symbol parameter is set to 'g', the extracted value is 0.001.

Parameters:

- symbol: The symbol of the dimension, e.g., 'm' for meter
- numberFormat: The IETF BCP 47 language tag, e.g. 'en'
- filter: Only extracts from values that contain the given regex (case-insensitive).
"""
)
case class PhysicalQuantityExtractor(symbol: String = "", numberFormat: String = "en", filter: String = "") extends Transformer {

  val numberParser = NumberFormat.getInstance(Locale.forLanguageTag(numberFormat))

  val filterRegex = if(filter.nonEmpty) Some(("(?i)" + filter).r) else None

  val dimensionRegex = s"([\\d\\.,]+)\\s*(\\w*)$symbol".r

  val unitPrefixes = Map(
    "p" -> 0.000000000001,
    "n" -> 0.000000001,
    "μ" -> 0.000001,
    "U" -> 0.000001,
    "u" -> 0.000001,
    "m" -> 0.0001,
    "c" -> 0.001,
    "d" -> 0.01,
    "da" -> 10.0,
    "h" -> 100.0,
    "k" -> 1000.0,
    "K" -> 1000.0,
    "M" -> 1000000.0,
    "G" -> 1000000000.0
  )

  override def apply(values: Seq[Seq[String]]) = {
    values.head.flatMap(evaluate)
  }

  def evaluate(value: String): Option[String] = {
    for(regex <- filterRegex if regex.findFirstIn(value).isEmpty)
      return None

    for(matches <- dimensionRegex.findFirstMatchIn(value)) yield {
      val number = numberParser.parse(matches.group(1)).doubleValue()
      val factor = unitPrefixes.getOrElse(matches.group(2), 1.0)
      (number * factor).toString
    }
  }

}
