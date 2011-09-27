package de.fuberlin.wiwiss.silk.linkspec.similarity

import scala.math.max

abstract class FlatIndexAggregator extends Aggregator {
  override def combineIndexes(indexSet1: Set[Seq[Int]], blockCounts1: Seq[Int],
                              indexSet2: Set[Seq[Int]], blockCounts2: Seq[Int]): Set[Seq[Int]] = {
    val newIndexSet1 = indexSet1.map(_.padTo(max(blockCounts1.size, blockCounts2.size), 0))
    val newIndexSet2 = indexSet2.map(_.zipAll(blockCounts2, 0, 0).map {
      case (indexValue, blockCount) => blockCount + indexValue
    })

    newIndexSet1 ++ newIndexSet2
  }

  override def combineBlockCounts(blockCounts1: Seq[Int], blockCounts2: Seq[Int]): Seq[Int] = {
    blockCounts1.zipAll(blockCounts2, 0, 0).map {
      case (c1, c2) => c1 + c2
    }
  }
}






