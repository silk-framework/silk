package org.silkframework.rule

import org.silkframework.entity.TypedPath
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.aggegrator.{MaximumAggregator, MinimumAggregator, NegationAggregator}
import org.silkframework.rule.similarity.{Aggregation, Aggregator, Comparison, SimilarityOperator}
import org.silkframework.util.Identifier

/**
  * A link spec as a boolean rule. This representation is only applicable to a subset of linking rules.
  */
case class BooleanLinkageRule(root: BooleanOperator) {
  def comparisons: Seq[BooleanComparisonOperator] = {
    comparisons(root)
  }

  def comparisons(operator: BooleanOperator): Seq[BooleanComparisonOperator] = {
    operator match {
      case BooleanAnd(children) =>
        children.flatMap(comparisons)
      case BooleanOr(children) =>
        children.flatMap(comparisons)
      case BooleanNot(child) =>
        comparisons(child)
      case comparison: BooleanComparisonOperator =>
        Seq(comparison)
    }
  }

  /** Turns the boolean linkage rule to conjunctive normal form, in order to better turn it into executable form.
    * CNF example: (A || !B || C) && (!A || B) && G */
  def toCNF: CnfBooleanAnd = {
    val negationNormalForm = applyDeMorgan(root)
    distributeOrOverAnd(negationNormalForm)
  }

  /** This expects the boolean expression to be in negation normal form.
    * This will distribute Or over And resulting in a conjunctive normal form (CNF).
    * The returned data structure is always an AND with one or more nested ORs. */
  private def distributeOrOverAnd(operator: BooleanOperator): CnfBooleanAnd = {
    operator match {
      case booleanNot: BooleanNot =>
        // We know that in negation normal form this must be a leaf
        CnfBooleanAnd(Seq(CnfBooleanOr(Seq(CnfBooleanLeafNot(booleanNot)))))
      case BooleanAnd(children) =>
        val processedChildren = distributeOrOverAnd(children)
        val merged = processedChildren.reduce((a, b) => CnfBooleanAnd(a.orClauses ++ b.orClauses))
        merged
      case BooleanOr(children) =>
        val processedChildren = distributeOrOverAnd(children)
        // since the result must be a CNF, just merge all children
        var calcDistributive = processedChildren.head
        for(nextAnd <- processedChildren.tail) {
          calcDistributive = CnfBooleanAnd(for(currentClauses <- calcDistributive.orClauses;
              andChild <- nextAnd.orClauses) yield {
            CnfBooleanOr(currentClauses.leaves ++ andChild.leaves)
          })
        }
        calcDistributive
      case bc: BooleanComparisonOperator =>
        // This is trivially a leaf
        CnfBooleanAnd(Seq(CnfBooleanOr(Seq(CnfBooleanLeafComparison(bc)))))
    }
  }

  private def distributeOrOverAnd(children: Seq[BooleanOperator]): Seq[CnfBooleanAnd] = {
    children.map(distributeOrOverAnd)
  }

  /** Applies De Morgan recursively to achieve negation normal form */
  private def applyDeMorgan(operator: BooleanOperator): BooleanOperator = {
    operator match {
      case BooleanNot(notChild) =>
        notChild match {
          case _: BooleanComparisonOperator => operator // We are done here
          case BooleanAnd(children) =>
            BooleanOr(children.map(c => applyDeMorgan(BooleanNot(c)))) // Apply De Morgan to children
          case BooleanOr(children) =>
            BooleanAnd(children.map(c => applyDeMorgan(BooleanNot(c))))
          case BooleanNot(subNotChild) =>
            applyDeMorgan(subNotChild) // Double negation elimination
        }
      case BooleanOr(children) => BooleanOr(children.map(applyDeMorgan))
      case BooleanAnd(children) => BooleanAnd(children.map(applyDeMorgan))
      case _: BooleanComparisonOperator => operator
    }
  }
}

sealed trait BooleanOperator

case class BooleanAnd(children: Seq[BooleanOperator]) extends BooleanOperator {
  assert(children.nonEmpty)
  override def toString: String = s"And(${children.map(_.toString).mkString(", ")})"
}

case class BooleanOr(children: Seq[BooleanOperator]) extends BooleanOperator {
  assert(children.nonEmpty)
  override def toString: String = s"Or(${children.map(_.toString).mkString(", ")})"
}

case class BooleanNot(child: BooleanOperator) extends BooleanOperator {
  override def toString: String = s"Not(${child.toString})"
}

/** Boolean operator that turns values into a (fuzzy) boolean value */
sealed trait ValueInputBooleanOutput extends BooleanOperator

case class BooleanComparisonOperator(id: Identifier,
                                     sourceOperator: ValueOutputOperator,
                                     targetOperator: ValueOutputOperator,
                                     comparison: Comparison) extends ValueInputBooleanOutput {
  override def toString: String = s"'$id'"
}

/** Helper classes to ensure KNF characteristics*/
case class CnfBooleanAnd(orClauses: Seq[CnfBooleanOr]) {
  def asBooleanOperator: BooleanAnd = BooleanAnd(orClauses.map(_.asBooleanOperator))
}

case class CnfBooleanOr(leaves: Seq[CnfBooleanLeaf]) {
  def asBooleanOperator: BooleanOr = BooleanOr(leaves.map(_.asBooleanOperator))
}

sealed trait CnfBooleanLeaf {
  def asBooleanOperator: BooleanOperator = booleanOperator

  def booleanOperator: BooleanOperator
}

case class CnfBooleanLeafNot(booleanOperator: BooleanNot) extends CnfBooleanLeaf {
  assert(booleanOperator.child.isInstanceOf[BooleanComparisonOperator], "Only comparison operators allowed as child of NOT operator in CNF.")

  def booleanComparison: BooleanComparisonOperator = booleanOperator.child.asInstanceOf[BooleanComparisonOperator]
}

case class CnfBooleanLeafComparison(booleanOperator: BooleanComparisonOperator) extends CnfBooleanLeaf

sealed trait ValueOutputOperator {
  /** The operator from the link spec */
  def inputOperator: Input
}

case class InputPathOperator(typedPath: TypedPath, inputOperator: Input) extends ValueOutputOperator

case class TransformationOperator(children: Seq[ValueOutputOperator], inputOperator: Input) extends ValueOutputOperator

object BooleanLinkageRule {
  /** Converts a link spec into a boolean linkage rule if it can be interpreted as a boolean expression. */
  def apply(linkSpec: LinkSpec): Option[BooleanLinkageRule] = {
    apply(linkSpec.rule)
  }

  def apply(linkageRule: LinkageRule): Option[BooleanLinkageRule] = {
    linkageRule.operator.flatMap { operator =>
      try {
        Some(BooleanLinkageRule(convert(operator)))
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
    BooleanComparisonOperator(comparison.id, convert(comparison.inputs.source), convert(comparison.inputs.target), comparison)
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

