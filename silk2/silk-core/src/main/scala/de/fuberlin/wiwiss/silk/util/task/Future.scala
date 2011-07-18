package de.fuberlin.wiwiss.silk.util.task

/**
 * Represents the result of an asynchronous computation.
 */
trait Future[+T] extends (() => T) {
  /**
   * Blocks until the computation to complete, and then retrieves its result.
   */
  override def apply(): T

  /**
   * Returns true if the result is available.
   */
  def isSet: Boolean
}

object Future {
  implicit def fromJavaFuture[T](future: java.util.concurrent.Future[T]) = {
    new Future[T] {
      def apply() = future.get

      def isSet = future.isDone
    }
  }
}