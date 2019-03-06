package org.silkframework.rule

import org.silkframework.entity.TypedPath
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.aggegrator.{MaximumAggregator, MinimumAggregator, NegationAggregator}
import org.silkframework.rule.similarity.{Aggregation, Aggregator, Comparison, SimilarityOperator}

/**
  * A link spec as a boolean rule. This representation is only applicable to a subset of linking rules.
  */
case class BooleanLinkSpec(root: BooleanOperator)

sealed trait BooleanOperator

case class BooleanAnd(children: Seq[BooleanOperator]) extends BooleanOperator

case class BooleanOr(children: Seq[BooleanOperator]) extends BooleanOperator

case class BooleanNot(child: BooleanOperator) extends BooleanOperator

/** Boolean operator that turns values into a (fuzzy) boolean value */
sealed trait ValueInputBooleanOutput extends BooleanOperator

case class BooleanSimilarityOperator(sourceOperator: ValueOutputOperator,
                                     targetOperator: ValueOutputOperator) extends ValueInputBooleanOutput

sealed trait ValueOutputOperator {
  /** The operator from the link spec */
  def inputOperator: Input
}

case class InputPathOperator(typedPath: TypedPath, inputOperator: Input) extends ValueOutputOperator

case class TransformationOperator(children: Seq[ValueOutputOperator], inputOperator: Input) extends ValueOutputOperator

object BooleanLinkSpec {
  /** Converts a link spec in a boolean link spec if it can be interpreted as a boolean expression. */
  def apply(linkSpec: LinkSpec): Option[BooleanLinkSpec] = {
    linkSpec.rule.operator.flatMap { operator =>
        try {
          Some(BooleanLinkSpec(convert(operator)))
        } catch {
          case _: NonBooleanConvertibleException =>
            None
        }

    }
  }

  def convert(similarityOperator: SimilarityOperator): BooleanOperator = {
    similarityOperator match {
      case agg: Aggregation =>
        convert(agg)
      case comparison: Comparison =>
        convert(comparison)
      case _ =>
        throw NonBooleanConvertibleException("Cannot convert similarity operator of type " + similarityOperator.getClass.getName)
    }
  }

  def convert(aggregation: Aggregation): BooleanOperator = {
    aggregation.aggregator match {
      case _: MinimumAggregator => BooleanAnd(aggregation.operators.map(convert))
      case _: MaximumAggregator => BooleanOr(aggregation.operators.map(convert))
      case _: NegationAggregator =>
        aggregation.children match {
          case Seq(child) =>
            BooleanNot(convert(child))
          case _ =>
            throw NonBooleanConvertibleException("Negation aggregator must have exactly 1 input. " + aggregation.id +
                " has " + aggregation.children.size)
        }
      case agg: Aggregator =>
        throw NonBooleanConvertibleException("Aggregators of type " + agg.getClass.getSimpleName + " cannot be converted to boolean expression!")
    }
  }

  def convert(comparison: Comparison): BooleanOperator = {
    BooleanSimilarityOperator(convert(comparison.inputs.source), convert(comparison.inputs.target))
  }

  def convert(input: Input): ValueOutputOperator = {
    input match {
      case pi: PathInput =>
        InputPathOperator(pi.path.asStringTypedPath, pi)
      case ti: TransformInput =>
        TransformationOperator(ti.inputs.map(convert), ti)
    }
  }

  case class NonBooleanConvertibleException(msg: String) extends IllegalArgumentException(msg)
}

