package de.fuberlin.wiwiss.silk.util.task

import collection.mutable.Publisher
import de.fuberlin.wiwiss.silk.util.task.ValueTask.ValueHolder

/**
 * A task where the intermediate results of the computation can be retrieved.
 */
abstract class ValueTask[T](initialValue: T) extends Task[T]  {
  /** Holds the current value of this task */
  val value = new ValueHolder(initialValue)
}

object ValueTask {
  /**
   * Holds a value.
   */
  class ValueHolder[T](initialValue: T) extends Publisher[ValueUpdated[T]] {
      @volatile private var currentValue = initialValue

      def get: T = currentValue

      def update(v: T) {
        currentValue = v
        publish(ValueUpdated(v))
      }
  }

  /**
   * Thrown if the current value is updated.
   */
  case class ValueUpdated[T](value: T)
}