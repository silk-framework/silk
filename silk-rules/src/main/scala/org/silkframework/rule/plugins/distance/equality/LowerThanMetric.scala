package org.silkframework.rule.plugins.distance.equality

import org.silkframework.rule.similarity.{NonSymmetricDistanceMeasure, SimpleDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{DistanceMeasurePlugin, DistanceMeasureRange, Param, Plugin}
import org.silkframework.util.StringUtils._

@Plugin(
  id = "lowerThan",
  categories = Array("Equality"),
  label = "Lower than",
  description = "Checks if the source value is lower than the target value."
)
@DistanceMeasurePlugin(
  range = DistanceMeasureRange.BOOLEAN
)
case class LowerThanMetric(@Param("Accept equal values")
                           orEqual: Boolean = false,
                           @Param("Per default, if both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used. Choose a more specific order for improved performance.")
                           order: OrderEnum = OrderEnum.autodetect,
                           @Param(value = "Reverse source and target inputs", advanced = true)
                           reverse: Boolean = false) extends SimpleDistanceMeasure with NonSymmetricDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    LowerThanMetric.evaluate(str1, str2, orEqual, order)
  }
}

object LowerThanMetric {

  @inline
  def evaluate(str1: String, str2: String, orEqual: Boolean, order: OrderEnum): Double = {
    order match {
      case OrderEnum.alphabetical =>
        LowerThanMetric.evaluateAlphabeticalOrder(str1, str2, orEqual)
      case OrderEnum.numerical =>
        LowerThanMetric.evaluateNumericalOrder(str1, str2, orEqual)
      case OrderEnum.integer =>
        LowerThanMetric.evaluateIntegerOrder(str1, str2, orEqual)
      case OrderEnum.autodetect =>
        LowerThanMetric.evaluateAutodetect(str1, str2, orEqual)
    }
  }

  @inline
  private def evaluateAlphabeticalOrder(str1: String, str2: String, orEqual: Boolean): Double = {
    if(orEqual) {
      if (str1 <= str2) 0.0 else 1.0
    } else {
      if (str1 < str2) 0.0 else 1.0
    }
  }

  @inline
  private def evaluateNumericalOrder(str1: String, str2: String, orEqual: Boolean): Double = {
    if(orEqual) {
      (str1, str2) match {
        case (DoubleLiteral(n1), DoubleLiteral(n2)) => if (n1 <= n2) 0.0 else 1.0
        case _ => 1.0
      }
    } else {
      (str1, str2) match {
        case (DoubleLiteral(n1), DoubleLiteral(n2)) => if (n1 < n2) 0.0 else 1.0
        case _ => 1.0
      }
    }
  }

  @inline
  private def evaluateIntegerOrder(str1: String, str2: String, orEqual: Boolean): Double = {
    if(orEqual) {
      (str1, str2) match {
        case (IntLiteral(n1), IntLiteral(n2)) => if (n1 <= n2) 0.0 else 1.0
        case _ => 1.0
      }
    } else {
      (str1, str2) match {
        case (IntLiteral(n1), IntLiteral(n2)) => if (n1 < n2) 0.0 else 1.0
        case _ => 1.0
      }
    }
  }

  @inline
  private def evaluateAutodetect(str1: String, str2: String, orEqual: Boolean): Double = {
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
