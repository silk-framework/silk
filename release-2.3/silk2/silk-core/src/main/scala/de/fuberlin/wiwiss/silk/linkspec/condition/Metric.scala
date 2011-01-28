package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Factory}

trait Metric extends Strategy
{
  def evaluate(value1 : String, value2 : String, threshold : Double) : Double

  def index(value : String, threshold : Double = 0.0) : Set[Seq[Int]] = Set(Seq(0))

  val blockCounts : Seq[Int] = Seq(1)

  //TODO replace by function blockIndex which calls index on default?
  protected def getBlocks(index : Seq[Double], overlap : Double) : Set[Seq[Int]] =
  {
    def addIndex(blockSet : Set[Seq[Int]], newIndex : (Double, Int)) : Set[Seq[Int]] =
    {
      for(blockSeq <- blockSet;
          newBlock <- getBlock(newIndex._1, newIndex._2, overlap)) yield
      {
        blockSeq :+ newBlock
      }
    }

    (index zip blockCounts).foldLeft(Set(Seq[Int]()))(addIndex)
  }

  /**
   * Retrieves the block which corresponds to a specific value.
   */
  private def getBlock(index : Double, blockCount : Int, overlap : Double) : Set[Int] =
  {
    val block = index * blockCount
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

object Metric extends Factory[Metric]
