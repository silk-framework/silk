package org.silkframework.serialization.json.metadata

/**
  * Utility class. Wrapper for exceptions that are missing metadata. Errors with missing features or null occurrences in
  * the serialization classes are avoided by using this.
  *
  * @param message Exception summary.
  */
case class UnknownCauseException(message: String) extends Throwable {
  /**
    * Has an unknown cause. That has no cause, but missing/null top level objects are avoided.
    *
    * @return UnknownCause
    */
  override def getCause: Throwable = UnknownCause(this.message)
}

case class UnknownCause(message: String) extends Throwable {
  /**
    * Has no cause. At some level a null here is normal and tested for.
    *
    * Quote: {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
    *
    * @return null
    */
  override def getCause: UnknownCause = null
}


