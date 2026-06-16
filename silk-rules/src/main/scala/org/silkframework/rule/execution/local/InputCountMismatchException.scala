package org.silkframework.rule.execution.local

/**
  * Thrown when the number of input entity tables provided to a transform operator does not match
  * the number expected by its rule schemata.
  */
case class InputCountMismatchException(errorMsg: String) extends RuntimeException(errorMsg)
