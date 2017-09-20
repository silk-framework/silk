package org.silkframework.runtime.resource

import org.silkframework.runtime.validation.NotFoundException

class ResourceNotFoundException(msg: String) extends NotFoundException(msg) {

  override val errorText = "Resource not found"
}
