package org.silkframework.runtime.resource

import java.io.{InputStream, OutputStream}

/**
  * A resource that cannot be written.
  */
class ReadOnlyResource(resource: Resource) extends WritableResource {

  override def name: String = resource.name

  override def path: String = resource.path

  override def exists = resource.exists

  override def load: InputStream = resource.load

  override def write(write: (OutputStream) => Unit): Unit = {
    throw new UnsupportedOperationException("This resource can not be written.")
  }
}
