package org.silkframework.runtime.validation

/**
  * Super class for all more specific 'not found' exceptions. This will automatically lead to a 404 response if thrown inside
  * a controller.
  */
class NotFoundException(msg: String) extends NoSuchElementException(msg)