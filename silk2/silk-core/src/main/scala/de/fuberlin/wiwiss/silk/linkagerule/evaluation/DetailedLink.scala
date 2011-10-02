package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.PathInput
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}

class DetailedLink(source: String,
                   target: String,
                   entities: Option[DPair[Entity]],
                   val details: Option[DetailedLink.Confidence]) extends Link(source, target, details.flatMap(_.value), entities) {

  def this(link: Link) = this(link.source, link.target, link.entities, link.confidence.map(c => DetailedLink.SimpleConfidence(Some(c))))
}

object DetailedLink {

  sealed trait Confidence {
    def value: Option[Double]
  }

  case class SimpleConfidence(value: Option[Double]) extends Confidence

  case class AggregatorConfidence(value: Option[Double], aggregation: Aggregation, children: Seq[Confidence]) extends Confidence

  case class ComparisonConfidence(value: Option[Double], comparison: Comparison, sourceInput: InputValue, targetInput: InputValue) extends Confidence

  case class InputValue(input: PathInput, values: Traversable[String])

}