package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.Index

@Plugin(
  id = "num",
  label = "Numeric similarity",
  description = "Computes the numeric distance between two numbers."
)
case class NumMetric(minValue: Double = Double.NegativeInfinity, maxValue: Double = Double.PositiveInfinity) extends SimpleDistanceMeasure {
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val maxBlockCount = 10000

  private val blockOverlap = 0.5

  private val indexEnabled = {
    if (minValue.isNegInfinity || maxValue.isPosInfinity) {
      logger.info("Blocking disabled for numeric comparison as minValue and maxValue is not defined")
      false
    }
    else {
      true
    }
  }

  override def evaluate(str1: String, str2: String, limit: Double) = {
    (str1, str2) match {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) => {
        abs(num1 - num2) / (limit)
      }
      case _ => Double.PositiveInfinity
    }
  }

  override def indexValue(str: String, limit: Double): Index = {
    if (indexEnabled) {
      str match {
        case DoubleLiteral(num) => Index.continuous(num, minValue, maxValue, limit)
        case _ => Index.empty
      }
    }
    else {
      Index.default
    }
  }

  private def blockCount(limit: Double) = {
    min(maxBlockCount, ((maxValue - minValue) / limit * blockOverlap).toInt)
  }
}
