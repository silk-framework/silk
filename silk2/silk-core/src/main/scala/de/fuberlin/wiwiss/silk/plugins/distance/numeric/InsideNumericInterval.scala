package de.fuberlin.wiwiss.silk.plugins.distance.numeric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "insideNumericInterval",
  label = "Inside numeric interval",
  categories = Array("Numeric"),
  description = "Checks if a number is contained inside a numeric interval, such as '1900 - 2000'"
)
case class InsideNumericInterval(separator: String = "—|–|-") extends SimpleDistanceMeasure {

  override def evaluate(value1: String, value2: String, limit: Double): Double = {
    (parseInterval(value1), parseInterval(value2)) match {
      case (Some(i1), Some(i2)) => insideInterval(i1, i2)
      case _ => 1.0

    }
  }

  private def insideInterval(i1: (Double, Double), i2: (Double, Double)) = {
    if(i1._1 == i2._1 || i1._2 == i2._2)
      0.0
    else if (i1._1 > i2._1) {
      if (i1._2 < i2._2)
        0.0
      else
        1.0
    } else {
      if (i1._2 > i2._2)
        0.0
      else
        1.0
    }
  }

  private def parseInterval(str: String): Option[(Double, Double)] = {
    val parts = str.split(separator, -1)
    if(parts.size == 1)
      parseNumber(parts(0)).map(n => (n, n))
    else if(parts.size == 2)
      for(n1 <- parseNumber(parts(0));
          n2 <- parseNumber(parts(1))) yield (n1, n2)
    else
      None
  }

  private def parseNumber(str: String) = {
    try {
      Some(str.filter(_.isDigit).toDouble)
    } catch {
      case ex: NumberFormatException => None
    }
  }
}
