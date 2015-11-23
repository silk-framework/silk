package org.silkframework.plugins.transformer.date

import javax.xml.datatype.DatatypeFactory

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.StringUtils.XSDDateLiteral

/**
 * Compares two dates.
 *
 * @author Robert Isele
 */
@Plugin(
  id = "compareDates",
  categories = Array("Date"),
  label = "Compare Dates",
  description =
    """ | Compares two dates.
      | Returns 1 if the comparison yields true and 0 otherwise.
      | If there are multiple dates in both sets, the comparator must be true for all dates.
      | e.g. {2014-08-02,2014-08-03} < {2014-08-03} yields 0 as not all dates in the first set are smaller than in the second.
      | Accepts one parameter:
      |   comparator: One of '<', '<=', '=', '>=', '>' """
)
case class CompareDatesTransformer(comparator: String = "<") extends Transformer {
  private val datatypeFactory = DatatypeFactory.newInstance()

  override def apply(values: Seq[Set[String]]): Set[String] = {
    // Collect all dates in milliseconds
    val n1 = values(0).collect { case XSDDateLiteral(d) => d.toGregorianCalendar.getTimeInMillis }
    val n2 = values(1).collect { case XSDDateLiteral(d) => d.toGregorianCalendar.getTimeInMillis }

    // Compare dates
    val result = comparator match {
      case _ if n1.isEmpty || n2.isEmpty => false
      case "<"  => n1.max < n2.min
      case "<=" => n1.max <= n2.min
      case ">"  => n1.min > n2.max
      case ">=" => n1.min >= n2.max
      case "="  => n1.min == n1.max && n2.min == n2.max && n1.head == n2.head
    }
    // Return result
    Set(if(result) "1" else "0")
  }
}
