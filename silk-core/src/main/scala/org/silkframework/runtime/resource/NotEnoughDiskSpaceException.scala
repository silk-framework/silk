package org.silkframework.runtime.resource

import java.io.IOException

/**
  * Thrown if there is not enough space left on the local file system.
  */
class NotEnoughDiskSpaceException(msg: String) extends IOException(msg)
