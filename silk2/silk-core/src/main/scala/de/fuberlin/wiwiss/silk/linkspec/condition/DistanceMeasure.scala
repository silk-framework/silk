package de.fuberlin.wiwiss.silk.linkspec.condition

import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Factory}

trait DistanceMeasure extends Strategy
{
  def apply(values1 : Traversable[String], values2 : Traversable[String], limit : Double = 0.0) : Double

  def index(value : String, limit : Double) : Set[Seq[Int]] = Set(Seq(0))

  def blockCounts(limit : Double) : Seq[Int] = Seq(1)

  //TODO replace by function blockIndex which calls index on default?
  protected def getBlocks(index : Seq[Double], overlap : Double, limit : Double) : Set[Seq[Int]] =
  {
    def addIndex(blockSet : Set[Seq[Int]], newIndex : (Double, Int)) : Set[Seq[Int]] =
    {
      for(blockSeq <- blockSet;
          newBlock <- getBlock(newIndex._1, newIndex._2, overlap)) yield
      {
        blockSeq :+ newBlock
      }
    }

    (index zip blockCounts(limit)).foldLeft(Set(Seq[Int]()))(addIndex)
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

/**
 * A simple similarity measure which compares pairs of values.
 */
trait SimpleDistanceMeasure extends DistanceMeasure
{
  def apply(values1 : Traversable[String], values2 : Traversable[String], limit : Double) : Double =
  {
    val distances = for (str1 <- values1; str2 <- values2) yield evaluate(str1, str2, limit)

    distances.min
  }

  /**
   * Evaluates the similarity of a pair of similarity values.
   */
  def evaluate(value1 : String, value2 : String, limit : Double = 0.0) : Double
}

object DistanceMeasure extends Factory[DistanceMeasure]
