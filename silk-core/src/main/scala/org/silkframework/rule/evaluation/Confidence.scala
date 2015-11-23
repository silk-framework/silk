package org.silkframework.rule.evaluation

import scala.xml.Node
import org.silkframework.rule.similarity.{Comparison, Aggregation}

sealed trait Confidence {
  def score: Option[Double]
  def toXML: Node
}

case class SimpleConfidence(score: Option[Double]) extends Confidence {
  def toXML =
      <SimpleConfidence score={score.toString}/>
}

case class AggregatorConfidence(score: Option[Double], aggregation: Aggregation, children: Seq[Confidence]) extends Confidence {
  def toXML =
    <AggregatorConfidence id={aggregation.id} score={score.toString}>
      { children.map(_.toXML) }
    </AggregatorConfidence>
}

case class ComparisonConfidence(score: Option[Double], comparison: Comparison, sourceValue: Value, targetValue: Value) extends Confidence {
  def toXML =
    <ComparisonConfidence id={comparison.id} score={score.toString} sourceValue={sourceValue.values.toString} targetValue={targetValue.values.toString}>
    </ComparisonConfidence>
}