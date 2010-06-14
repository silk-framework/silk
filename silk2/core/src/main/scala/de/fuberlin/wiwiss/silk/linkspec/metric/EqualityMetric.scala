package de.fuberlin.wiwiss.silk.linkspec.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric

class EqualityMetric(val params : Map[String, String] = Map.empty) extends Metric
{
    override def evaluate(str1 : String, str2 : String) = if(str1 == str2) 1.0 else 0.0
}
