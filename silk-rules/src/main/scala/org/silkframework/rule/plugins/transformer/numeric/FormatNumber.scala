package org.silkframework.rule.plugins.transformer.numeric

import java.text.{DecimalFormat, NumberFormat}
import java.util.Locale

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.StringUtils.DoubleLiteral

@Plugin(
  id = "formatNumber",
  categories = Array("Numeric"),
  label = "Format number",
  description =
"""
  Formats a number according to a user-defined pattern.
  The pattern syntax is documented at:
  https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html
"""
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("pattern", "000"),
    input1 = Array("1"),
    output = Array("001")
  ),
  new TransformExample(
    parameters = Array("pattern", "000000.000"),
    input1 = Array("123.78"),
    output = Array("000123.780")
  ),
  new TransformExample(
    parameters = Array("pattern", "###,###.###"),
    input1 = Array("123456.789"),
    output = Array("123,456.789")
  ),
  new TransformExample(
    parameters = Array("pattern", "###.###,###", "locale", "de"),
    input1 = Array("123456.789"),
    output = Array("123.456,789")
  ),
  new TransformExample(
    parameters = Array("pattern", "# apples"),
    input1 = Array("10"),
    output = Array("10 apples")
  ),
  new TransformExample(
    parameters = Array("pattern", "000'0'"),
    input1 = Array("1"),
    output = Array("0010")
  )
))
case class FormatNumber(pattern: String, locale: String = "en") extends SimpleTransformer {

  private val format = NumberFormat.getNumberInstance(Locale.forLanguageTag(locale))
  format.asInstanceOf[DecimalFormat].applyLocalizedPattern(pattern)

  override def evaluate(value: String): String = {
    value match {
      case DoubleLiteral(d) => format.format(d)
      case _ => throw new ValidationException(s"Input value $value must be a number.")
    }
  }
}
