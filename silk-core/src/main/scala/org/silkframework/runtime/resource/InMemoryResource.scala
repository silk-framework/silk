package org.silkframework.runtime.resource

import java.io.{ByteArrayInputStream, InputStream}
import java.time.Instant

/**
 * A resource that is stored in memory.
 */
class InMemoryResource(override val name: String, override val path: String, bytes: Array[Byte]) extends Resource {

  /**
   * In-memory resources are always considered to exist.
   */
  override def exists: Boolean = true

  /**
   * Returns the size of this resource in bytes.
   */
  override def size: Option[Long] = {
    Some(bytes.length)
  }

  /**
   * The time that the resource was last modified.
   * Returns None if the time is not known.
   */
  override def modificationTime: Option[Instant] = None

  /**
   * Creates an input stream for reading the resource.
   *
   * @return An input stream for reading the resource.
   *         The caller is responsible for closing the stream after reading.
   */
  override def inputStream: InputStream = {
    new ByteArrayInputStream(bytes)
  }
}
