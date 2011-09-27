package de.fuberlin.wiwiss.silk.linkagerule.similarity

abstract class MultiIndexAggregator extends Aggregator {
  override def combineIndexes(indexSet1: Set[Seq[Int]], blockCounts1: Seq[Int],
                              indexSet2: Set[Seq[Int]], blockCounts2: Seq[Int]): Set[Seq[Int]] = {
    val indexes1 = if (indexSet1.isEmpty) Set(Seq.fill(blockCounts1.size)(0)) else indexSet1
    val indexes2 = if (indexSet2.isEmpty) Set(Seq.fill(blockCounts2.size)(0)) else indexSet2

    for (index1 <- indexes1;
         index2 <- indexes2) yield {
      index1 ++ index2
    }
  }

  override def combineBlockCounts(blockCounts1: Seq[Int], blockCounts2: Seq[Int]): Seq[Int] = {
    blockCounts1 ++ blockCounts2
  }
}






