package org.silkframework.serialization.json.metadata

case class UnknownCauseException(message: String) extends Throwable {

  override def getCause(): Throwable = this

}



