package de.fuberlin.wiwiss.silk.plugins.distance.equality

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils._

@Plugin(
  id = "lowerThan",
  categories = Array("Equality"),
  label = "LowerThan",
  description = "Return 1 if the source value is lower than the target " +
  "value, 0 otherwise. If both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used")
case class LowerThanMetric(orEqual: Boolean = false) extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double) = {
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
