package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._

class NumMetric(val params : Map[String, String] = Map.empty) extends Metric
{
    private val threshold = readRequiredDoubleParam("threshold")

    override def evaluate(str1 : String, str2 : String) =
    {
        (str1, str2) match
        {
            case (DoubleLiteral(num1), DoubleLiteral(num2)) => max((threshold - abs(num1 - num2)) / threshold, 0.0)
            case _ => 0.0
        }
    }
}
