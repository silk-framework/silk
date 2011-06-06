package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.condition.SimpleSimilarityMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(
  id = "num",
  label = "Numeric similarity",
  description = "Computes the numeric distance between two numbers and normalizes it using the maxDistance." +
    " The similarity score is 0.0 if the distance is bigger than maxDistance.")
class NumMetric(maxDistance : Double, minValue : Double = Double.NegativeInfinity, maxValue : Double = Double.PositiveInfinity) extends SimpleSimilarityMeasure
{
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val maxBlockCount = 10000

  private val blockOverlap = 0.5

  private val indexEnabled =
  {
    if(minValue.isNegInfinity || maxValue.isPosInfinity)
    {
      logger.info("Blocking disabled for numeric comparison as minValue and maxValue is not defined")
      false
    }
    else
    {
      true
    }
  }

  override def evaluate(str1 : String, str2 : String, threshold : Double) =
  {
    (str1, str2) match
    {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) =>
      {
        if(maxDistance == 0.0)
        {
          if(num1 == num2) 1.0 else 0.0
        }
        else
        {
          max(1.0 - abs(num1 - num2) / maxDistance, 0.0)
        }
      }
      case _ => 0.0
    }
  }

  override def index(str : String, threshold : Double) : Set[Seq[Int]] =
  {
    if(indexEnabled)
    {
      str match
      {
        case DoubleLiteral(num) =>
        {
          getBlocks(Seq((num - minValue).toDouble / maxValue), blockOverlap)
        }
        case _ => Set.empty
      }
    }
    else
    {
      Set(Seq(0))
    }
  }

  override val blockCounts : Seq[Int] =
  {
    if(indexEnabled)
    {
      Seq(1)
    }
    else
    {
      Seq(blockCount)
    }
  }

  private val blockCount =
  {
    if(maxDistance == 0.0)
    {
      maxBlockCount
    }
    else
    {
      min(maxBlockCount, (blockOverlap * (maxValue - minValue) / maxDistance).toInt)
    }
  }
}
