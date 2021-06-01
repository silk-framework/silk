package org.silkframework.workspace

import org.silkframework.runtime.validation.ValidationException

/**
  * Thrown if a task is created/updated that would create a circular dependency.
  *
  * @param circularTaskChain The labels of the tasks in the circular chain.
  */
case class CircularDependencyException(circularTaskChain: Seq[String])
  extends ValidationException(s"Task contains a circular dependency: ${circularTaskChain.mkString("'", "'->'", "'")}.")
