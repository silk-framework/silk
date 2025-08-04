package org.silkframework.runtime.resource

import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
  * A resource that cannot be written.
  */
case class ReadOnlyResource(resource: Resource) extends WritableResource {

  override def name: String = resource.name

  override def path: String = resource.path

  override def entryPath: Option[String] = resource.entryPath

  override def exists: Boolean = resource.exists

  override def size: Option[Long] = resource.size

  override def modificationTime: Option[Instant] = resource.modificationTime

  override def inputStream: InputStream = resource.inputStream

  override def createOutputStream(append: Boolean): OutputStream = {
    throw new UnsupportedOperationException("This resource can not be written.")
  }

  override def toString: String = resource.toString

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = throw new UnsupportedOperationException("This resource is read-only cannot be deleted.")
}
