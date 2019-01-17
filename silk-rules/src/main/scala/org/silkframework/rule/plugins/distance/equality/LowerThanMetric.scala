package org.silkframework.rule.plugins.distance.equality

import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.StringUtils._

@Plugin(
  id = "lowerThan",
  categories = Array("Equality"),
  label = "Lower than",
  description = "Checks if the source value is lower than the target value. " +
  "If both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used")
case class LowerThanMetric(orEqual: Boolean = false) extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    LowerThanMetric.evaluate(str1, str2, orEqual)
  }
}

object LowerThanMetric {

  def evaluate(str1: String, str2: String, orEqual: Boolean): Double = {
    if(orEqual) {
      (str1, str2) match {
        case (DoubleLiteral(n1), DoubleLiteral(n2)) => if (n1 <= n2) 0.0 else 1.0
        case _ => if (str1 <= str2) 0.0 else 1.0
      }
    } else {
      (str1, str2) match {
        case (DoubleLiteral(n1), DoubleLiteral(n2)) => if (n1 < n2) 0.0 else 1.0
        case _ => if (str1 < str2) 0.0 else 1.0
      }
    }
  }

}
