package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(
  id = "num",
  label = "Numeric Similarity",
  description = "Computes the numeric distance between two numbers and normalizes it using the threshold." +
    " The similarity score is 0.0 if the distance is bigger than threshold.")
class NumMetric(maxDistance : Double, minValue : Double, maxValue : Double) extends Metric
{
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val blockOverlap = 0.5

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
        getBlocks(Seq((num - minValue).toDouble / maxValue), blockOverlap)
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
    (blockOverlap * (maxValue - minValue) / maxDistance).toInt
  }
}
