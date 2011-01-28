package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "inequality", label = "Inequality", description = "Return 0 if strings are equal, 1 otherwise.")
class InequalityMetric() extends Metric
{
  override def evaluate(str1 : String, str2 : String, threshold : Double) = if(str1 == str2) 0.0 else 1.0
}
