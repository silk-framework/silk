package de.fuberlin.wiwiss.silk.plugins.transformer.numeric

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils.DoubleLiteral

/**
 * Compares the numbers of two operators.
 *
 * @author Robert Isele
 */
@Plugin(
  id = "compareNumbers",
  categories = Array("Numeric"),
  label = "Compare Numbers",
  description =
    """ | Compares the numbers of two sets.
      | Returns 1 if the comparison yields true and 0 otherwise.
      | If there are multiple numbers in both sets, the comparator must be true for all numbers.
      | e.g. {1,2} < {2,3} yields 0 as not all numbers in the first set are smaller than in the second.
      | Accepts one parameter:
      |   comparator: One of '<', '<=', '=', '>=', '>' """
)
class CompareNumbersTransformer(comparator: String = "<") extends Transformer {
  require(Set("<", "<=", "=", ">=", ">") contains comparator, "Comparator must be one of '<', '<=', '=', '>=', '>'")

  def apply(values: Seq[Set[String]]): Set[String] = {
    // Collect all numbers
    val n1 = values(0).collect { case DoubleLiteral(d) => d }
    val n2 = values(1).collect { case DoubleLiteral(d) => d }
    // Compare numbers
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