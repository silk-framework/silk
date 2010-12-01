package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import java.util.logging.Logger

class NumMetric(val params : Map[String, String] = Map.empty) extends Metric
{
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val minValue = readOptionalDoubleParam("min")
  private val maxValue = readOptionalDoubleParam("max")
  private val thresholdDistance = readRequiredDoubleParam("thresholdDistance")
  private val blockOverlap = 0.5

  if(minValue.isEmpty || maxValue.isEmpty)
  {
    logger.warning("Numeric Comparision does not define min and max values. Indexing disabled.")
  }

  override def evaluate(str1 : String, str2 : String, threshold : Double) =
  {
    (str1, str2) match
    {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) => max(1.0 - abs(num1 - num2) / thresholdDistance * (1.0 - threshold), 0.0)
      case _ => 0.0
    }
  }

  override def index(str : String, threshold : Double) : Set[Seq[Int]] =
  {
    str match
    {
      case DoubleLiteral(num) =>
      {
        (minValue, maxValue) match
        {
          case (Some(minV), Some(maxV)) =>
          {
            getBlocks((num - minV).toDouble / maxV)
          }
          case _ => Set(Seq(0))
        }
      }
      case _ => Set.empty
    }
  }

  override val blockCounts : Seq[Int] =
  {
    Seq(blockCount)
  }

  private val blockCount =
  {
    (minValue, maxValue) match
    {
      case (Some(minV), Some(maxV)) => (blockOverlap * (maxV - minV) / thresholdDistance).toInt
      case _ => 1
    }
  }

  /**
   * Retrieves the block which corresponds to a specific value.
   */
  private def getBlocks(value : Double) : Set[Seq[Int]] =
  {
    val overlap = 0.25

    val block = value * blockCount
    val blockIndex = block.toInt

    if(block <= 0.5)
    {
      Set(Seq(0))
    }
    else if(block >= blockCount - 0.5)
    {
      Set(Seq(blockCount - 1))
    }
    else
    {
      if(block - blockIndex < overlap)
      {
        Set(Seq(blockIndex), Seq(blockIndex - 1))
      }
      else if(block + 1 - blockIndex < overlap)
      {
        Set(Seq(blockIndex), Seq(blockIndex + 1))
      }
      else
      {
        Set(Seq(blockIndex))
      }
    }
  }
}
