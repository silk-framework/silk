package org.silkframework.runtime.resource

import java.io.IOException

/**
  * Thrown if a resource is too large to be loaded into memory.
  */
class ResourceTooLargeException(msg: String) extends IOException(msg)
