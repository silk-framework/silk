package de.fuberlin.wiwiss.silk.linkspec.similarity

import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Factory}
import scala.math.min

trait DistanceMeasure extends Strategy {
  //TODO accept set instead of traversable?
  def apply(values1: Traversable[String], values2: Traversable[String], limit: Double = Double.PositiveInfinity): Double

  def index(values: Set[String], limit: Double): Set[Seq[Int]] = Set(Seq(0))

  def blockCounts(limit: Double): Seq[Int] = Seq(1)

  //TODO replace by function blockIndex which calls index on default?
  protected def getBlocks(index: Seq[Double], overlap: Double, limit: Double): Set[Seq[Int]] = {
    def addIndex(blockSet: Set[Seq[Int]], newIndex: (Double, Int)): Set[Seq[Int]] = {
      for (blockSeq <- blockSet;
           newBlock <- getBlock(newIndex._1, newIndex._2, overlap)) yield {
        blockSeq :+ newBlock
      }
    }

    (index zip blockCounts(limit)).foldLeft(Set(Seq[Int]()))(addIndex)
  }

  /**
   * Retrieves the block which corresponds to a specific value.
   */
  private def getBlock(index: Double, blockCount: Int, overlap: Double): Set[Int] = {
    val block = index * blockCount
    val blockIndex = block.toInt

    if (block <= 0.5) {
      Set(0)
    }
    else if (block >= blockCount - 0.5) {
      Set(blockCount - 1)
    }
    else {
      if (block - blockIndex < overlap) {
        Set(blockIndex, blockIndex - 1)
      }
      else if (block + 1 - blockIndex < overlap) {
        Set(blockIndex, blockIndex + 1)
      }
      else {
        Set(blockIndex)
      }
    }
  }
}

/**
 * A simple similarity measure which compares pairs of values.
 */
trait SimpleDistanceMeasure extends DistanceMeasure {
  override final def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    var minDistance = Double.MaxValue

    for (str1 <- values1; str2 <- values2) {
      val distance = evaluate(str1, str2, min(limit, minDistance))
      minDistance = min(minDistance, distance)
    }

    minDistance
  }

  override final def index(values: Set[String], limit: Double): Set[Seq[Int]] =  {
    values.flatMap(value => indexValue(value, limit))
  }

  /**
   * Computes the similarity of a pair of values.
   */
  def evaluate(value1: String, value2: String, limit: Double = Double.PositiveInfinity): Double

  /**
   * Computes the index of a single value.
   */
  def indexValue(value: String, limit: Double): Set[Seq[Int]] = Set(Seq(0))
}

object DistanceMeasure extends Factory[DistanceMeasure]
