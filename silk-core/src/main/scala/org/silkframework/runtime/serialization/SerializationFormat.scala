package org.silkframework.runtime.serialization

trait SerializationFormat[T, U] {

  /**
    * Deserializes a value.
    */
  def read(value: U)(implicit readContext: ReadContext): T

  /**
    * Serializes a value.
    */
  def write(value: T)(implicit writeContext: WriteContext[U]): U

}
