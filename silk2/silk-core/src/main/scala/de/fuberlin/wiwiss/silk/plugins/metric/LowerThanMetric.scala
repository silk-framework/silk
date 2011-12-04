package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "lowerThan", label = "LowerThan", description = "Return 1 if the source value is lower than the target " +
  "value, 0 otherwise.")
case class LowerThanMetric() extends SimpleDistanceMeasure {
  override def evaluate(str1: String, str2: String, threshold: Double) = if (str1 < str2) 1.0 else 0.0
}
