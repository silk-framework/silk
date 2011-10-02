package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}
import de.fuberlin.wiwiss.silk.entity.Index

trait Aggregator extends AnyPlugin {
  def evaluate(weightedValues: Traversable[(Int, Double)]): Option[Double]

  /**
   * Combines two indexes into one.
   */
  def combineIndexes(index1: Index, index2: Index): Index

  def computeThreshold(threshold: Double, weight: Double): Double = {
    threshold
  }
}

object Aggregator extends PluginFactory[Aggregator]
