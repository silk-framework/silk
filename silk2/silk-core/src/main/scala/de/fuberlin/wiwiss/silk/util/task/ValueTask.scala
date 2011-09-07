package de.fuberlin.wiwiss.silk.util.task

import de.fuberlin.wiwiss.silk.util.task.ValueTask.ValueHolder
import de.fuberlin.wiwiss.silk.util.Observable

/**
 * A task where the intermediate results of the computation can be retrieved.
 */
abstract class ValueTask[T](initialValue: T) extends Task[T]  {
  /** Holds the current value of this task */
  val value = new ValueHolder(initialValue)

  /**
   * Returns a traversable which executes the task in the background and iterates through all values.
   */
  def toTraversable = new Traversable[T] {
    def foreach[U](f: T => U) = {
      val listener = (v: T) => f(v)
      value.onUpdate(listener)
      apply()
    }
  }

  def executeSubValueTask(subTask: ValueTask[T], finalProgress: Double = 1.0): T = {
    val listener = (v: T) => value.update(v)
    subTask.value.onUpdate(listener)
    executeSubTask(subTask, finalProgress)
  }
}

object ValueTask {
  /**
   * Holds a value.
   */
  class ValueHolder[T](initialValue: T) extends Observable[T] {
      @volatile private var currentValue = initialValue

      def get: T = currentValue

      def update(v: T) {
        currentValue = v
        publish(v)
      }
  }
}