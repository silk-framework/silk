package org.silkframework.runtime.resource

import java.io.ByteArrayInputStream

object EmptyResource extends Resource {
  /**
    * The local name of this resource.
    */
  override def name = "EmptyResource"

  /**
    * The path of this resource.
    */
  override def path = ""

  /**
    * Checks if this resource exists.
    */
  override def exists = true

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  override def size = Some(0L)

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  override def modificationTime = None

  /**
    * Loads the resource.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream = new ByteArrayInputStream(Array.empty)
}
