package org.silkframework.serialization.json.metadata

class UnknownCause extends Throwable {

  def apply(message: String) {
    super(message)
  }
  override def getCause(): Throwable = this

}



