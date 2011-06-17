package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.condition.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(
  id = "num",
  label = "Numeric similarity",
  description = "Computes the numeric distance between two numbers and normalizes it using the maxDistance." +
    " The similarity score is 0.0 if the distance is bigger than maxDistance.")
class NumMetric(minValue : Double = Double.NegativeInfinity, maxValue : Double = Double.PositiveInfinity, maxDistance : Double = Double.NaN) extends SimpleDistanceMeasure
{
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val scale = maxDistance match
  {
    case Double.NaN =>
    {
      1.0
    }
    case _ =>
    {
      logger.warning("The use of the 'maxDistance' parameter on the num metric is deprecated.\n" +
        "Please use the threshold paramter on the comparison instead.\n" +
        "Example: <Compare metric=\"num\" threshold=\"...\">")

      maxDistance
    }
  }

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

  override def evaluate(str1 : String, str2 : String, limit : Double) =
  {
    (str1, str2) match
    {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) =>
      {
        abs(num1 - num2) / (limit * scale)
      }
      case _ => Double.PositiveInfinity
    }
  }

  override def index(str : String, limit : Double) : Set[Seq[Int]] =
  {
    if(indexEnabled)
    {
      str match
      {
        case DoubleLiteral(num) =>
        {
          getBlocks(Seq((num - minValue).toDouble / maxValue), blockOverlap, limit * scale)
        }
        case _ => Set.empty
      }
    }
    else
    {
      Set(Seq(0))
    }
  }

  override def blockCounts(limit : Double) : Seq[Int] =
  {
    if(indexEnabled)
    {
      Seq(1)
    }
    else
    {
      Seq(blockCount(limit * scale))
    }
  }

  private def blockCount(limit : Double) =
  {
    min(maxBlockCount, ((maxValue - minValue) / limit * blockOverlap).toInt)
  }
}
