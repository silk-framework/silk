package org.silkframework.rule.input

import org.silkframework.util.Identifier

/**
  * Holds the values returned by an input operator.
  */
case class Value(values: Seq[String], errors: Iterable[OperatorEvaluationError] = None)

case class OperatorEvaluationError(operatorId: Identifier, error: Throwable)
