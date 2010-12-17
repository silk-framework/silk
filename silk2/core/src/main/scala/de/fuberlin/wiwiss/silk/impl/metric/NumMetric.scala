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
  private val maxDistance = readRequiredDoubleParam("maxDistance")
  private val blockOverlap = 0.5

  if(minValue.isEmpty || maxValue.isEmpty)
  {
    logger.warning("Numeric Comparision does not define min and max values. Indexing disabled.")
  }

  override def evaluate(str1 : String, str2 : String, threshold : Double) =
  {
    (str1, str2) match
    {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) => max(1.0 - abs(num1 - num2) / maxDistance, 0.0)
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
            getBlocks(Seq((num - minV).toDouble / maxV), blockOverlap)
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
      case (Some(minV), Some(maxV)) => (blockOverlap * (maxV - minV) / maxDistance).toInt
      case _ => 1
    }
  }
}
