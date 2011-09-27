package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.util.plugin.{PluginFactory, AnyPlugin}

trait Aggregator extends AnyPlugin {
  def evaluate(weightedValues: Traversable[(Int, Double)]): Option[Double]

  /**
   * Combines two indexes into one.
   */
  def combineIndexes(indexSet1: Set[Seq[Int]], blockCounts1: Seq[Int],
                     indexSet2: Set[Seq[Int]], blockCounts2: Seq[Int]): Set[Seq[Int]]

  /**
   * Combines two block counts into one.
   */
  def combineBlockCounts(blockCounts1: Seq[Int], blockCounts2: Seq[Int]): Seq[Int]

  def computeThreshold(threshold: Double, weight: Double): Double = {
    threshold
  }
}

object Aggregator extends PluginFactory[Aggregator]
