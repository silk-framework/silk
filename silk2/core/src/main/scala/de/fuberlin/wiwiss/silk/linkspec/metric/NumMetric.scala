package de.fuberlin.wiwiss.silk.linkspec.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._

class NumMetric(val params : Map[String, String] = Map.empty) extends Metric
{
    override def evaluate(str1 : String, str2 : String) =
    {
        (str1, str2) match
        {
            case (DoubleLiteral(num1), DoubleLiteral(num2)) => min((num1/num2), (num2/num1))
            case _ => 0.0
        }
    }
}
