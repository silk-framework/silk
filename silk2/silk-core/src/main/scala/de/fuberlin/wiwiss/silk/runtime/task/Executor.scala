package de.fuberlin.wiwiss.silk.runtime.task

/**
 * Executes tasks.
 */
trait Executor {

  def execute(task: Task): TaskControl

}

/**
 * Holds the global executor.
 */
object Executor {

  private val executor = new DefaultExecutor

  /**
   * Retrieves an executor instance.
   */
  def apply(): Executor = {
    executor
  }
}

