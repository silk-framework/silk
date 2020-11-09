package org.silkframework.learning

/**
  * Thrown if the learning fails due to an unrecoverable error.
  */
class LearningException(msg: String, cause: Option[Throwable] = None) extends Exception(msg, cause.orNull)
