package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.{Index, Operator}

/**
 * An aggregation combines multiple similarity values into a single value.
 */
case class Aggregation(id: Identifier = Operator.generateId,
                       required: Boolean = false,
                       weight: Int = 1,
                       aggregator: Aggregator,
                       operators: Seq[SimilarityOperator]) extends SimilarityOperator {
  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The similarity threshold.
   *
   * @return The similarity as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    val totalWeights = operators.map(_.weight).sum

    val weightedValues = operators.collect{op =>
      op(entities, aggregator.computeThreshold(limit, op.weight.toDouble / totalWeights)) match {
        case Some(v) => (op.weight, v)
        case None if op.required => return None
      }
    }

    aggregator.evaluate(weightedValues)
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  override def index(entity: Entity, threshold: Double): Index = {
    val totalWeights = operators.map(_.weight).sum

    val indexSets = {
      for (op <- operators) yield {
        val index = op.index(entity, aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights))

        if (op.required && index.isEmpty) return Index.empty;

        index
      }
    }

    if (indexSets.isEmpty)
      Index.empty
    else
      indexSets.reduceLeft[Index](aggregator.combineIndexes(_, _))
  }

  override def toXML(implicit prefixes: Prefixes) = aggregator match {
    case Aggregator(func, params) => {
      <Aggregate id={id} required={required.toString} weight={weight.toString} type={func}>
        {operators.map(_.toXML)}
      </Aggregate>
    }
  }
}

object Aggregation {
  def fromXML(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Aggregation = {
    val requiredStr = node \ "@required" text
    val weightStr = node \ "@weight" text

    val aggregator = Aggregator(node \ "@type" text, Operator.readParams(node))

    Aggregation(
      id = Operator.readId(node),
      required = if (requiredStr.isEmpty) false else requiredStr.toBoolean,
      weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
      operators = SimilarityOperator.fromXML(node.child),
      aggregator = aggregator
    )
  }
}
