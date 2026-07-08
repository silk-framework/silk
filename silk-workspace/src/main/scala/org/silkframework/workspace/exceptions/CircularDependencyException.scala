package org.silkframework.workspace.exceptions

import org.silkframework.runtime.validation.TaskValidationException

/**
  * Thrown if a task is created/updated that would create a circular dependency.
  *
  * @param circularTaskChain The labels of the tasks in the circular chain.
  */
case class CircularDependencyException(circularTaskChain: Seq[String])
  extends TaskValidationException(s"Task contains a circular dependency: ${circularTaskChain.mkString("'", "'->'", "'")}.")
