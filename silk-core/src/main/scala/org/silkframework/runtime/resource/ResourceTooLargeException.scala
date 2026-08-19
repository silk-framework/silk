package org.silkframework.runtime.resource

import java.io.IOException

/**
  * Thrown if a resource is too large to be loaded into memory.
  */
class ResourceTooLargeException(msg: String) extends IOException(msg) {

  def this(name: String, size: Long) = {
    this(s"Resource $name is too large to be loaded into memory (size: $size, maximum size: ${Resource.maxInMemorySize()}). " +
      s"Configure '${Resource.maxInMemorySizeParameterName}' in order to increase this limit.")
  }
}
