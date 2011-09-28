package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.Index

@Plugin(id = "equality", label = "Equality", description = "Return 1 if strings are equal, 0 otherwise.")
class EqualityMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double) = if (str1 == str2) 0.0 else 1.0

  override def indexValue(str: String, threshold: Double) = Index.oneDim(Set(str.hashCode))
}
