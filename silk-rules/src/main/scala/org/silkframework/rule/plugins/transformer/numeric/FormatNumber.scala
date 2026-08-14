package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import java.text.{DecimalFormat, NumberFormat}
import java.util.Locale

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.plugin.types.autoComlpetionProviders.LocaleParameterAutoCompletionProvider
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.StringUtils.DoubleLiteral

@Plugin(
  id = FormatNumber.pluginId,
  categories = Array("Numeric"),
  label = "Format number",
  description =
"""
  Formats a number according to a user-defined pattern.
  The pattern syntax is documented at:
  https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html
""",
  relatedPlugins = Array(
    new PluginReference(
      id = PhysicalQuantityExtractor.pluginId,
      description = "Format number requires a numeric input. If the source data contains quantity strings with embedded unit symbols, Extract physical quantity parses those strings and returns the numeric value in the base unit — the form that Format number can then render according to a decimal pattern."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "The digit '0' in the pattern pads the number with leading zeros.",
    parameters = Array("pattern", "000"),
    input1 = Array("1"),
    output = Array("001")
  ),
  new TransformExample(
    description = "Padding applies to the integer and the fraction part.",
    parameters = Array("pattern", "000000.000"),
    input1 = Array("123.78"),
    output = Array("000123.780")
  ),
  new TransformExample(
    description = "The placeholder '#' stands for an optional digit and ',' inserts a grouping separator.",
    parameters = Array("pattern", "###,###.###"),
    input1 = Array("123456.789"),
    output = Array("123,456.789")
  ),
  new TransformExample(
    description = "The pattern is interpreted in the configured locale, here German with '.' as grouping and ',' as decimal separator.",
    parameters = Array("pattern", "###.###,###", "locale", "de"),
    input1 = Array("123456.789"),
    output = Array("123.456,789")
  ),
  new TransformExample(
    description = "Literal text in the pattern is kept in the output.",
    parameters = Array("pattern", "# apples"),
    input1 = Array("10"),
    output = Array("10 apples")
  ),
  new TransformExample(
    description = "Quoted characters are copied to the output as-is, even if they are digits.",
    parameters = Array("pattern", "000'0'"),
    input1 = Array("1"),
    output = Array("0010")
  ),
  new TransformExample(
    description = "A pattern without fraction digits rounds to a whole number.",
    parameters = Array("pattern", "0"),
    input1 = Array("1.0"),
    output = Array("1")
  ),
  new TransformExample(
    description = "Leading zeros of the input are removed unless the pattern demands them.",
    parameters = Array("pattern", "0.0"),
    input1 = Array("0000123.4"),
    output = Array("123.4")
  )
))
case class FormatNumber(@Param("The number pattern, e.g., '###,###.###'.")
                        pattern: String,
                        @Param(value = "The locale in which the pattern is interpreted, given as an IETF BCP 47 language tag, e.g., 'en'.",
                               autoCompletionProvider = classOf[LocaleParameterAutoCompletionProvider])
                        locale: String = "en") extends SimpleTransformer {

  // Fail eagerly on invalid patterns at construction time.
  createFormat()

  // NumberFormat is not thread-safe, so each thread gets its own instance.
  @transient private lazy val format: ThreadLocal[NumberFormat] = ThreadLocal.withInitial(() => createFormat())

  private def createFormat(): NumberFormat = {
    val format = NumberFormat.getNumberInstance(Locale.forLanguageTag(locale))
    format.asInstanceOf[DecimalFormat].applyLocalizedPattern(pattern)
    format
  }

  override def evaluate(value: String): String = {
    value match {
      case DoubleLiteral(d) => format.get().format(d)
      case _ => throw new ValidationException(s"Input value $value must be a number.")
    }
  }
}

object FormatNumber {
  final val pluginId = "formatNumber"
}
