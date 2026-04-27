package org.silkframework.rule.plugins.transformer.numeric

import org.silkframework.rule.input.InlineTransformer
import org.silkframework.rule.plugins.transformer.ComparatorEnum
import org.silkframework.rule.plugins.transformer.date.CompareDatesTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}
import org.silkframework.util.StringUtils.DoubleLiteral

/**
 * Compares the numbers of two input sets.
 */
@Plugin(
  id = CompareNumbersTransformer.pluginId,
  categories = Array("Numeric"),
  label = "Compare numbers",
  description =
"""Compares the numbers of two sets.
Returns 1 if the comparison yields true and 0 otherwise.
If there are multiple numbers in both sets, the comparator must be true for all numbers.
For instance, {1,2} < {2,3} yields 0 as not all numbers in the first set are smaller than in the second.""",
  relatedPlugins = Array(
    new PluginReference(
      id = CompareDatesTransformer.pluginId,
      description = "Compare numbers and Compare dates both return 1 or 0 by applying the same ordering and equality comparators across two input sets. Compare dates is not a date-aware extension of Compare numbers — each plugin accepts only its own value type, doubles for one and XSD date literals for the other."
    )
  )
)
case class CompareNumbersTransformer(comparator: ComparatorEnum = ComparatorEnum.less) extends InlineTransformer {

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    // Collect all numbers
    val n1 = values(0).collect { case DoubleLiteral(d) => d }
    val n2 = values(1).collect { case DoubleLiteral(d) => d }
    // Compare numbers
    val result = comparator match {
      case _ if n1.isEmpty || n2.isEmpty => false
      case ComparatorEnum.less  => n1.max < n2.min
      case ComparatorEnum.lessEqual => n1.max <= n2.min
      case ComparatorEnum.greater  => n1.min > n2.max
      case ComparatorEnum.greaterEqual => n1.min >= n2.max
      case ComparatorEnum.equal  => n1.min == n1.max && n2.min == n2.max && n1.head == n2.head
    }
    // Return result
    Seq(if(result) "1" else "0")
  }
}

object CompareNumbersTransformer {
  final val pluginId = "compareNumbers"
}
