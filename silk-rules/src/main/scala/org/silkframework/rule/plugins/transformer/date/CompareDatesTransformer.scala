package org.silkframework.rule.plugins.transformer.date

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.ComparatorEnum
import org.silkframework.util.StringUtils.XSDDateLiteral
import org.silkframework.rule.plugins.transformer.ComparatorEnum._
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

/**
 * Compares two dates.
 */
@Plugin(
  id = "compareDates",
  categories = Array("Date"),
  label = "Compare dates",
  description =
"""Compares two dates.
Returns 1 if the comparison yields true and 0 otherwise.
If there are multiple dates in both sets, the comparator must be true for all dates.
For instance, {2014-08-02,2014-08-03} < {2014-08-03} yields 0 as not all dates in the first set are smaller than in the second."""
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
