package org.silkframework.rule.similarity

/**
  * An aggregator that only aggregates concrete values, i.e. it cannot cope with missing/unknown values.
  */
trait ConcreteValueAggregator extends Aggregator {
  protected def evaluateConcreteValues(weightedValues: Traversable[(Int, Double)]): Option[Double]

//  override def evaluate(weightedValues: Traversable[(Int, Option[Double])]): Option[Double] = {
//    if(weightedValues.exists(_._2.isEmpty)) {
//      None // TODO: Is this correct??
//    } else {
//      evaluateConcreteValues(weightedValues.map {case (weigth, value) => (weigth, value.get)})
//    }
//  }
}
