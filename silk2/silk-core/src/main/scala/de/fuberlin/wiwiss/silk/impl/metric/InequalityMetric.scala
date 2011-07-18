package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "inequality", label = "Inequality", description = "Return 0 if strings are equal, 1 otherwise.")
class InequalityMetric() extends SimpleDistanceMeasure
{
  override def evaluate(str1 : String, str2 : String, threshold : Double) = if(str1 == str2) 1.0 else 0.0
}
