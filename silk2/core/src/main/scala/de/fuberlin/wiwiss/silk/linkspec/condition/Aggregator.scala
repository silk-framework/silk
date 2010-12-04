package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}
import scala.math.max

trait Aggregator extends Strategy
{
  def evaluate(weightedValues : Traversable[(Int, Double)]) : Option[Double]

  /**
   * Combines two indexes into one.
   */
  def combineIndexes(indexSet1 : Set[Seq[Int]], blockCounts1 : Seq[Int],
                     indexSet2 : Set[Seq[Int]], blockCounts2 : Seq[Int]) : Set[Seq[Int]]

  /**
   * Combines two block counts into one.
   */
  def combineBlockCounts(blockCounts1 : Seq[Int], blockCounts2 : Seq[Int]) : Seq[Int]
}

object Aggregator extends Factory[Aggregator]

trait FlatIndexAggregator extends Aggregator
{
  override def combineIndexes(indexSet1 : Set[Seq[Int]], blockCounts1 : Seq[Int],
                              indexSet2 : Set[Seq[Int]], blockCounts2 : Seq[Int]) : Set[Seq[Int]] =
  {
    val newIndexSet1 = indexSet1.map(_.padTo(max(blockCounts1.size, blockCounts2.size), 0))
    val newIndexSet2 = indexSet2.map(_.zipAll(blockCounts2, 0, 0).map{ case (indexValue, blockCount) => blockCount + indexValue })

    newIndexSet1 ++ newIndexSet2
  }


  override def combineBlockCounts(blockCounts1 : Seq[Int], blockCounts2 : Seq[Int]) : Seq[Int] =
  {
    blockCounts1.zipAll(blockCounts2, 0, 0).map{case (c1, c2) => c1 + c2}
  }
}

trait MultiIndexAggregator extends Aggregator
{
  override def combineIndexes(indexSet1 : Set[Seq[Int]], blockCounts1 : Seq[Int],
                              indexSet2 : Set[Seq[Int]], blockCounts2 : Seq[Int]) : Set[Seq[Int]] =
  {
    for(index1 <- indexSet1;
        index2 <- indexSet2) yield
    {
      index1 ++ index2
    }
  }

  override def combineBlockCounts(blockCounts1 : Seq[Int], blockCounts2 : Seq[Int]) : Seq[Int] =
  {
    blockCounts1 ++ blockCounts2
  }
}
