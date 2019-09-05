package org.silkframework.runtime.resource
import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
  * A Resource with a list of predefined types overriding any automatically determined types.
  * If multiple typed are contained with an resource, their implicit ordering has to align with the
  * order of this sequence.
  */
case class ResourceWithKnownTypes(resource: WritableResource, knownTypes: IndexedSeq[String]) extends WritableResource {
  /**
    * The local name of this resource.
    */
  override def name: String = resource.name

  /**
    * The path of this resource.
    */
  override def path: String = resource.path

  /**
    * Checks if this resource exists.
    */
  override def exists: Boolean = resource.exists

  /**
    * Returns the size of this resource in bytes.
    * Returns None if the size is not known.
    */
  override def size: Option[Long] = resource.size

  /**
    * The time that the resource was last modified.
    * Returns None if the time is not known.
    */
  override def modificationTime: Option[Instant] = resource.modificationTime

  /**
    * Creates an input stream for reading the resource.
    *
    * @return An input stream for reading the resource.
    *         The caller is responsible for closing the stream after reading.
    */
  override def inputStream: InputStream = resource.inputStream

  /**
    * Preferred method for writing to a resource.
    *
    * @param write A function that accepts an output stream and writes to it.
    */
  override def write(append: Boolean)(write: OutputStream => Unit): Unit = resource.write(append)(write)

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = resource.delete()
}
