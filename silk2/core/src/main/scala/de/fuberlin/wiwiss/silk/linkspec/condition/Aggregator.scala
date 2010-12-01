package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait Aggregator extends Strategy
{
  def evaluate(weightedValues : Traversable[(Int, Double)]) : Option[Double]

  def aggregateIndexes(indexSets : Traversable[Set[Seq[Int]]]) : Set[Seq[Int]]

  def aggregateBlockCounts(blockCounts : Traversable[Seq[Int]]) : Seq[Int]
}

object Aggregator extends Factory[Aggregator]

trait FlatIndexAggregator extends Aggregator
{
  override def aggregateIndexes(indexSets : Traversable[Set[Seq[Int]]]) : Set[Seq[Int]] =
  {
    val maxDimension = indexSets.flatMap(_.headOption.map(_.size)).max

    for(indexSet <- indexSets;
        index <- indexSet) yield
    {
      index.padTo(maxDimension, 0)
    }
  }.toSet

  override def aggregateBlockCounts(blockCounts : Traversable[Seq[Int]]) : Seq[Int] =
  {
    //TODO indexes nacheinander statt uebereinander anordnen?

    //Combines two blockCounts into one
    def combine(blocks1 : Seq[Int], blocks2 : Seq[Int]) : Seq[Int] =
    {
      blocks1.zipAll(blocks2, Int.MaxValue, Int.MaxValue).map{case (c1, c2) => math.min(c1, c2)}
    }

    blockCounts.reduceLeft(combine)
  }
}

trait MultiIndexAggregator extends Aggregator
{
  override def aggregateIndexes(indexSets : Traversable[Set[Seq[Int]]]) : Set[Seq[Int]] =
  {
    def combine(indexSet1 : Set[Seq[Int]], indexSet2 : Set[Seq[Int]]) : Set[Seq[Int]] =
    {
      for(index1 <- indexSet1;
          index2 <- indexSet2) yield
      {
        index1 ++ index2
      }
    }

    indexSets.reduceLeft(combine)
  }

  override def aggregateBlockCounts(blockCounts : Traversable[Seq[Int]]) : Seq[Int] =
  {
    blockCounts.reduceLeft(_ ++ _)
  }
}