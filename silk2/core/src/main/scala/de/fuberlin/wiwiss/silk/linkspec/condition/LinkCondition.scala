package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class LinkCondition(val rootAggregation : Aggregation)
{
  def apply(sourceInstance : Instance, targetInstance : Instance) : Double =
  {
    rootAggregation(sourceInstance, targetInstance).headOption.getOrElse(0.0)
  }

  //TODO flatten high dimensional indexes
  def index(instance : Instance) : Set[Int] =
  {
    //The indexes as vectors of doubles
    val indexes = rootAggregation.index(instance)

    //The indexes as vectors of sets of block indexes (one double index may translate to multiple block indexes)
    val multiBlockIndexes =
    {
      for(index <- indexes) yield
      {
         for((block, blockCount) <- index zip rootAggregation.blockCounts) yield
         {
           getBlock(block, blockCount)
         }
      }
    }

    //The indexes as vectors of block indexes
    val blockIndexes = multiBlockIndexes.flatMap(flattenIndexes)

    //Convert the index vector to a scalar
    val result = for(index <- blockIndexes) yield
    {
      (index.tail zip rootAggregation.blockCounts).foldLeft(index.head){case (iLeft, (iRight, blocks)) => iLeft * blocks + iRight}
    }

    result
  }

  val blockCount =
  {
    rootAggregation.blockCounts.foldLeft(1)(_ * _)
  }

  private val overlap = 0.25

  private def flattenIndexes(multiIndex : Seq[Set[Int]]) : Set[Seq[Int]] =
  {
    def flattenIndexesRecursive(flattendIndex : Set[Seq[Int]], multiIndex : Seq[Set[Int]]) : Set[Seq[Int]] =
    {
      if(multiIndex.isEmpty)
      {
        flattendIndex
      }
      else
      {
        val flatIndexes = flattendIndex.flatMap(flatIndex => multiIndex.head.map(flatIndex :+ _))
        flattenIndexesRecursive(flatIndexes, multiIndex.tail)
      }
    }

    flattenIndexesRecursive(multiIndex.head.flatMap(block => Set(Seq(block))), multiIndex.tail)
  }

  /**
   * Retrieves the block which corresponds to a specific value.
   */
  private def getBlock(value : Double, blockCount : Int) : Set[Int] =
  {
    val block = value * blockCount
    val blockIndex = block.toInt

    if(block <= 0.5)
    {
      Set(0)
    }
    else if(block >= blockCount - 0.5)
    {
      Set(blockCount - 1)
    }
    else
    {
      if(block - blockIndex < overlap)
      {
        Set(blockIndex, blockIndex - 1)
      }
      else if(block + 1 - blockIndex < overlap)
      {
        Set(blockIndex, blockIndex + 1)
      }
      else
      {
        Set(blockIndex)
      }
    }
  }
}
