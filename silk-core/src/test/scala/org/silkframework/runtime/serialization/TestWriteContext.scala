package org.silkframework.runtime.serialization

/** Write context to be used in tests. */
object TestWriteContext {
  def apply[T](): WriteContext[T] = WriteContext.empty[T]
}
