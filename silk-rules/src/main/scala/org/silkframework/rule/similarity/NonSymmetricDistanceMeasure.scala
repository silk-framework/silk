package org.silkframework.rule.similarity

import org.silkframework.entity.Index

/**
  * A distance measure that yields different results for distance(A,B) and distance(B,A).
  * Adds a property ''reverse'' that reverses source and target values.
  */
trait NonSymmetricDistanceMeasure extends DistanceMeasure {

  /** Reverse source and target inputs. */
  def reverse: Boolean

  /**
    * Computes the distance between two sets of strings.
    *
    * @param values1 The first sequence of strings.
    * @param values2 The second sequence of strings.
    * @param limit If the expected distance between both sets exceeds this limit, this method may
    *              return Double.PositiveInfinity instead of the actual distance in order to save computation time.
    * @return A positive number that denotes the computed distance between both strings.
    */
  abstract override def apply(values1: Seq[String], values2: Seq[String], limit: Double = Double.PositiveInfinity): Double = {
    if(reverse) {
      super.apply(values2, values1, limit)
    } else {
      super.apply(values1, values2, limit)
    }
  }

  override def index(values: Seq[String], limit: Double, sourceOrTarget: Boolean): Index = {
    if(reverse) {
      super.index(values, limit, !sourceOrTarget)
    } else {
      super.index(values, limit, sourceOrTarget)
    }
  }

}
