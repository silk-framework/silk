package org.silkframework.rule.plugins.transformer.date

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.ComparatorEnum
import org.silkframework.util.StringUtils.XSDDateLiteral
import org.silkframework.rule.plugins.transformer.ComparatorEnum._
import org.silkframework.rule.plugins.transformer.numeric.CompareNumbersTransformer
import org.silkframework.rule.plugins.transformer.validation.ValidateRegex
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

/**
 * Compares two dates.
 */
@Plugin(
  id = CompareDatesTransformer.pluginId,
  categories = Array("Date"),
  label = "Compare dates",
  description = """Compares two dates.""",
  documentationFile = "CompareDatesTransformer.md",
  relatedPlugins = Array(
    new PluginReference(
      id = ValidateRegex.pluginId,
      description = "The Compare dates plugin filters both inputs down to valid XSD date literals and returns 1 or 0 based on the comparator, returning 0 when one side contains no valid date at all. The Validate regex plugin enforces a pattern as a hard boundary and fails when a value does not conform, rather than turning invalid input into a 0 result."
    ),
    new PluginReference(
      id = CompareNumbersTransformer.pluginId,
      description = "Compare dates applies ordering and equality comparators to XSD date literals and returns 1 or 0. Compare numbers applies the same comparators to doubles — the two plugins are not interchangeable, as each rejects the other's input type entirely."
    ),
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("comparator", "<"),
    input1 = Array("2017-01-01"),
    input2 = Array("2017-01-02"),
    output = Array("1")
  ),
  new TransformExample(
    parameters = Array("comparator", "<"),
    input1 = Array("2017-01-02"),
    input2 = Array("2017-01-01"),
    output = Array("0")
  ),
  new TransformExample(
    parameters = Array("comparator", ">"),
    input1 = Array("2017-01-02"),
    input2 = Array("2017-01-01"),
    output = Array("1")
  ),
  new TransformExample(
    parameters = Array("comparator", ">"),
    input1 = Array("2017-01-01"),
    input2 = Array("2017-01-02"),
    output = Array("0")
  ),
  new TransformExample(
    parameters = Array("comparator", "="),
    input1 = Array("2017-01-01"),
    input2 = Array("2017-01-01"),
    output = Array("1")
  ),
  new TransformExample(
    parameters = Array("comparator", "="),
    input1 = Array("2017-01-02"),
    input2 = Array("2017-01-01"),
    output = Array("0")
  )
))
case class CompareDatesTransformer(comparator: ComparatorEnum = ComparatorEnum.less) extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    // Collect all dates in milliseconds
    val n1 = values(0).collect { case XSDDateLiteral(d) => d.toGregorianCalendar.getTimeInMillis }
    val n2 = values(1).collect { case XSDDateLiteral(d) => d.toGregorianCalendar.getTimeInMillis }

    // Compare dates
    val result = comparator match {
      case _ if n1.isEmpty || n2.isEmpty =>
        false
      case ComparatorEnum.less =>
        n1.max < n2.min
      case ComparatorEnum.lessEqual =>
        n1.max <= n2.min
      case ComparatorEnum.greater =>
        n1.min > n2.max
      case ComparatorEnum.greaterEqual =>
        n1.min >= n2.max
      case ComparatorEnum.equal =>
        n1.min == n1.max && n2.min == n2.max && n1.head == n2.head
    }
    // Return result
    Seq(if(result) "1" else "0")
  }
}

object CompareDatesTransformer {
  final val pluginId = "compareDates"
}
