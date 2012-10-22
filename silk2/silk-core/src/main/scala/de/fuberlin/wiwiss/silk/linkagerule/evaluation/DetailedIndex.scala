package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.entity.{Index, Link, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import xml.Node

/**
 * Holds the indices from a linkage rule and all its suboperators.
 *
 * @param index The overall index
 * @param entity The entity from which the index has been build
 * @param root The index of the root operator
 */
case class DetailedIndex(index: Index,
                         entity: Entity,
                         root: Option[DetailedIndex.OperatorIndex]) {

  def toXML: Node = root.map(_.toXML).getOrElse(<NoIndex/>)
}

object DetailedIndex {

  /**
   * The index of a similarity operator.
   * May either be an AggregatorIndex or an ComparisonIndex.
   */
  sealed trait OperatorIndex {
    def index: Index
    def toXML: Node
  }

  /**
   * An index built from an aggregation.
   *
   * @param index The index
   * @param aggregation The aggregation from which the index has been built
   * @param children The child operators of the aggregation
   */
  case class AggregationIndex(index: Index, aggregation: Aggregation, children: Seq[OperatorIndex]) extends OperatorIndex {
    def toXML =
      <AggregationIndex id={aggregation.id} index={index.toString}>
        { children.map(_.toXML) }
      </AggregationIndex>
  }

  /**
   * An index built from a comparison.
   *
   * @param index The index
   * @param values The values from which the index has been built
   * @param comparison The comparison from which the index has been built
   */
  case class ComparisonIndex(index: Index, values: Set[String], comparison: Comparison) extends OperatorIndex {
    def toXML =
      <ComparisonIndex id={comparison.id} index={index.toString} values={values.mkString("|")} >
      </ComparisonIndex>
  }
}
