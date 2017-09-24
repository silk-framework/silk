package org.silkframework.runtime.resource

import java.io.{InputStream, OutputStream}

/**
  * A resource that cannot be written.
  */
case class ReadOnlyResource(resource: Resource) extends WritableResource {

  override def name: String = resource.name

  override def path: String = resource.path

  override def exists = resource.exists

  override def size = resource.size

  override def modificationTime = resource.modificationTime

  override def inputStream: InputStream = resource.inputStream

  override def write(append: Boolean = false)(write: (OutputStream) => Unit): Unit = {
    throw new UnsupportedOperationException("This resource can not be written.")
  }

  override def toString = resource.toString

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = throw new UnsupportedOperationException("This resource is read-only cannot be deleted.")
}
