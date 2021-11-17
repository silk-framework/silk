package org.silkframework.plugins.dataset.hierarchical

/**
  * Thrown if the depth of the written structure exceeds the configured limit.
  */
class MaxDepthExceededException(message: String) extends RuntimeException(message)
