package org.silkframework.runtime.resource

import java.io.OutputStream

/**
  * A resource that cannot be written.
  */
case class ReadOnlyResource(resource: Resource) extends WritableResource with ForwardingResource {

  override protected def underlying: Resource = resource

  override def createOutputStream(append: Boolean): OutputStream = {
    throw new UnsupportedOperationException("This resource can not be written.")
  }

  /**
    * Deletes this resource.
    */
  override def delete(): Unit = throw new UnsupportedOperationException("This resource is read-only cannot be deleted.")
}
