package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import xml.Node

case class Aggregation(id: Identifier = Operator.generateId, required: Boolean, weight: Int,
                       operators: Seq[SimilarityOperator], aggregator: Aggregator) extends SimilarityOperator {
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

    val weightedValues = {
      for (operator <- operators) yield {
        val value = operator(entities, aggregator.computeThreshold(limit, operator.weight.toDouble / totalWeights))
        if (operator.required && value.isEmpty) return None

        (operator.weight, value.getOrElse(-1.0))
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
  override def index(entity: Entity, threshold: Double): Set[Seq[Int]] = {
    val totalWeights = operators.map(_.weight).sum

    val indexSets = {
      for (op <- operators) yield {
        val index = op.index(entity, aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights))
        val blockCounts = op.blockCounts(threshold)

        if (op.required && index.isEmpty) return Set.empty;

        (index, blockCounts)
      }
    }

    if (indexSets.isEmpty) {
      Set.empty
    }
    else {
      val combined = indexSets.reduceLeft[(Set[Seq[Int]], Seq[Int])] {
        case ((indexSet1, blockCounts1), (indexSet2, blockCounts2)) => {
          val combinedIndexSet = aggregator.combineIndexes(indexSet1, blockCounts1, indexSet2, blockCounts2)
          val combinedBlockCounts = aggregator.combineBlockCounts(blockCounts1, blockCounts2)

          (combinedIndexSet, combinedBlockCounts)
        }
      }

      combined._1
    }
  }

  /**
   * The number of blocks in each dimension of the index.
   */
  override def blockCounts(threshold: Double): Seq[Int] = {
    val totalWeights = operators.map(_.weight).sum

    operators.map(op => op.blockCounts(aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights)))
      .foldLeft(Seq[Int]())((blockCounts1, blockCounts2) => aggregator.combineBlockCounts(blockCounts1, blockCounts2))
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
